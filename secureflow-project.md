# SecureFlow — Multi-Tenant Authorization Platform

## Cos'è il progetto

SecureFlow è una piattaforma self-hosted per la gestione centralizzata di autenticazione, autorizzazione e rate limiting delle API. Permette ad un'azienda di controllare chi può accedere alle proprie API, con quali permessi, e con quali limiti — il tutto tracciato in un audit log in tempo reale.

Il problema che risolve è reale e comune: ogni team che espone API a clienti esterni deve rispondere a tre domande fondamentali:

- **Chi sei?** — autenticazione tramite API key o token
- **Cosa puoi fare?** — autorizzazione basata su ruoli e permessi granulari
- **Quanto puoi farlo?** — rate limiting per tenant configurabile

SecureFlow centralizza queste tre responsabilità in un unico sistema, ispezionabile tramite dashboard e completamente auto-ospitato.

### Analogia pratica

È paragonabile a un pannello di controllo tipo Stripe (per la gestione delle API key) combinato con il rate limiting di Cloudflare, installabile su qualsiasi server senza costi di licenza.

---

## Valore tecnico del progetto

Il progetto non nasce per competere con Kong o AWS API Gateway. Nasce per dimostrare padronanza su un insieme di problemi tecnici che ricorrono nei sistemi distribuiti reali:

- Concorrenza e stato condiviso (rate limiting distribuito)
- Security architecture (RBAC gerarchico, tenant isolation)
- Reactive programming (gateway non bloccante)
- Event-driven design (audit log asincrono)
- Operational maturity (CI/CD, health checks, structured logging)

---

## Stack tecnologico

### Backend — Gateway Service
**Spring WebFlux (reactive)**

Il gateway è il punto di ingresso di tutte le richieste. Usa il modello reattivo di Spring WebFlux invece del modello tradizionale thread-per-request. Questo significa che può gestire migliaia di connessioni concorrenti con un numero limitato di thread, senza bloccare in attesa di I/O.

Responsabilità:
- Ricevere ogni richiesta in ingresso
- Validare la API key o il JWT token
- Controllare il rate limit per il tenant
- Forwarding verso il servizio destinatario
- Pubblicare l'evento di audit su Kafka

### Backend — Core Service
**Spring Boot 3.5 (imperativo, LTS)**

Il cervello del sistema. Gestisce tutta la logica di business: tenant, utenti, ruoli, permessi, API key. Usa il modello tradizionale Spring MVC, più semplice da leggere e mantenere per operazioni che non richiedono reattività.

Responsabilità:
- CRUD per tenant, utenti, ruoli, permessi
- RBAC engine con `AuthorizationManager` custom
- Generazione e rotazione delle API key
- Webhook delivery verso sistemi esterni
- Consumo degli eventi Kafka per persistere l'audit log

### Autenticazione — Keycloak
**Keycloak 26.5 (self-hosted, LTS)**

Keycloak è un **Identity Provider (IdP)** open source: un sistema centralizzato che gestisce l'autenticazione degli utenti, così che le singole applicazioni non debbano implementare login, gestione password o storage utenti. In SecureFlow gestisce **solo gli utenti della dashboard** (chi accede all'interfaccia admin) — **non le API key**, che sono gestite dal Core Service.

**Cosa fa Keycloak:**
- **Single Sign-On (SSO)**: l'utente fa login una volta e accede a tutte le applicazioni collegate senza riautenticarsi
- **Emissione di token**: produce JWT conformi a OpenID Connect / OAuth 2.0
- **User management**: creazione utenti, ruoli, federazione con LDAP/Active Directory o social login (Google, GitHub, ecc.)
- **Standard di sicurezza**: implementa OAuth 2.0, OpenID Connect, SAML 2.0

Responsabilità in SecureFlow:
- Login/logout per gli utenti della dashboard
- Emissione di JWT token con claims custom (tenantId, ruoli)
- PKCE flow per il frontend Next.js
- Client credentials flow per comunicazione Machine-to-Machine tra i servizi

### Database — MySQL 8.4
**MySQL 8.4 LTS con row-level tenant isolation**

Ogni tabella che contiene dati di un tenant ha una colonna `tenant_id`. Un Hibernate Filter applicato globalmente assicura che nessuna query possa mai leggere dati di un tenant diverso da quello autenticato. Questo è il meccanismo di tenant isolation.

Flyway gestisce le migration versionate: ogni modifica allo schema è un file SQL numerato, eseguito automaticamente all'avvio, tracciato su Git.

### Cache e Rate Limiting — Redis 8
**Redis 8 con sliding window algorithm**

Il rate limiting distribuito è uno dei problemi più interessanti del progetto. Il contatore non può vivere in memoria del singolo processo perché in un deployment multi-istanza ogni processo avrebbe il proprio contatore, permettendo di aggirare il limite.

Redis risolve questo: il contatore è centralizzato. L'algoritmo usato è la sliding window, più preciso del fixed window counter perché non permette burst al confine tra due finestre temporali.

Redis viene usato anche come cache per i token JWT già validati, evitando una chiamata a Keycloak ad ogni richiesta.

### Messaging — Apache Kafka
**Kafka 4.x (KRaft) per audit events**

Ogni richiesta che passa dal gateway genera un evento di audit (chi, cosa, quando, risultato). Questo evento non viene scritto direttamente su MySQL dal gateway per due motivi: non si vuole aggiungere latenza alla richiesta, e non si vuole accoppiare il gateway al database.

Kafka disaccoppia i due: il gateway pubblica l'evento sul topic e prosegue. Il Core Service lo consuma in modo asincrono e lo persiste. Se il Core Service è temporaneamente down, gli eventi restano su Kafka finché non può processarli.

**Nota:** da Kafka 4.0 (marzo 2025) **Zookeeper non è più necessario**. Kafka usa esclusivamente la modalità **KRaft** (Kafka Raft): il consensus e la gestione dei metadata sono integrati in Kafka stesso, senza componenti esterni. Setup più semplice, failover più veloce, fino a ~1.9M partizioni per cluster.

### Reverse Proxy — Nginx
**Nginx come entry point**

Un singolo entry point che riceve tutto il traffico esterno. Termina TLS, serve i file statici del frontend Next.js, e fa proxy verso il gateway e il core service in base al path. Nasconde la topologia interna al client.

### Frontend — Next.js 16
**Next.js 16 LTS con App Router e React Server Components**

L'interfaccia di amministrazione della piattaforma. Usa Next.js 16 con App Router: le pagine che non richiedono interattività vengono renderizzate sul server (React Server Components), quelle interattive usano Client Components con TanStack Query per la gestione dello stato server.

Responsabilità:
- Login tramite Keycloak (PKCE flow)
- Dashboard metriche in tempo reale (SSE dal gateway)
- Gestione tenant, utenti, ruoli, permessi
- Gestione API key (generazione, revoca, rotazione)
- Audit log viewer con filtri e streaming live

### Containerizzazione — Docker
**Docker Compose per sviluppo, Dockerfile ottimizzati per produzione**

Ogni servizio ha il proprio Dockerfile multi-stage: uno stage per la build, uno stage finale minimale per l'immagine di runtime. Questo riduce la dimensione delle immagini e la superficie di attacco.

Docker Compose orchestra tutti i servizi in locale con un singolo comando. Le variabili di configurazione sensibili (password, secret) vengono passate tramite file `.env` mai committato su Git.

### CI/CD — GitHub Actions
**Pipeline automatizzata su ogni PR e merge**

Due workflow separati:
- `ci.yml` — eseguito su ogni pull request: compila, esegue i test con Testcontainers (istanze reali di MySQL, Redis e Kafka nei test), builda le immagini Docker
- `cd.yml` — eseguito su merge in main: pusha le immagini su Docker Hub con tag versione

Testcontainers è la scelta chiave per i test di integrazione: invece di mock, i test parlano con database e broker reali avviati come container temporanei durante il test run. I test sono quindi affidabili e vicini al comportamento reale.

### Documentazione — Architecture Decision Records
**ADR nella cartella `/docs/adr`**

Ogni scelta tecnologica non ovvia viene documentata con un ADR: un documento breve che spiega il contesto, le opzioni valutate, la decisione presa e il motivo. È la firma di chi lavora a livello di architect, non solo di developer.

---

## Struttura del repository

```
secureflow/
├── README.md
├── docker-compose.yml
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── cd.yml
├── backend/
│   ├── gateway-service/
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── core-service/
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── devops/
│   ├── mysql/
│   │   └── init.sql
│   ├── nginx/
│   │   └── nginx.conf
│   ├── keycloak/
│   │   └── realm-export.json
│   └── flyway/
│       └── sql/
├── docs/
│   ├── architecture.md
│   └── adr/
│       ├── 001-spring-webflux-for-gateway.md
│       ├── 002-keycloak-over-custom-auth.md
│       ├── 003-kafka-for-audit-events.md
│       └── 004-redis-sliding-window-rate-limit.md
└── secureflow-project.md
```

---

## Costo totale

**€0.** Tutto open source, tutto containerizzato, tutto eseguibile in locale.

Per un deploy pubblico con URL live: Oracle Cloud Free Tier offre una VM ARM con 24GB RAM permanentemente gratuita (richiede carta di credito per la registrazione, non addebita nulla sul piano free).

---

## Fasi di implementazione

---

### Fase 1 — Infrastruttura locale

**Obiettivo:** tutti i servizi di supporto funzionanti in Docker, raggiungibili e configurati.

Attività:
- Scrivere il `docker-compose.yml` con MySQL, Redis, Kafka (modalità KRaft, senza Zookeeper), Keycloak e Nginx
- Configurare Keycloak: creare il realm `secureflow`, il client per il frontend (PKCE), il client per il Core Service (client credentials), e i mapper custom per includere `tenantId` nel JWT
- Esportare la configurazione Keycloak in `realm-export.json` per rendere il setup riproducibile
- Configurare Flyway con la prima migration: schema iniziale per le tabelle `tenant`, `user`, `role`, `permission`
- Verificare la connettività tra tutti i container

Risultato atteso: `docker-compose up` porta online l'intero stack di supporto, Keycloak è accessibile e configurato, il database ha lo schema base.

---

### Fase 2 — Core Service: modello dati e RBAC engine

**Obiettivo:** il cuore del sistema — tenant isolation e autorizzazione granulare.

Attività:
- Implementare le entità JPA: `Tenant`, `User`, `Role`, `Permission`, `RoleAssignment`, `ApiKey`
- Implementare il Hibernate Filter per tenant isolation: ogni query viene automaticamente filtrata per `tenant_id` in base al contesto autenticato corrente
- Sviluppare il RBAC engine: `AuthorizationManager` custom in Spring Security 6 che consulta il database per verificare se l'utente corrente ha il permesso richiesto
- Creare l'annotation custom `@RequiresPermission("resource:action")` e il relativo aspect
- Implementare i primi endpoint REST: CRUD per tenant, utenti, ruoli e permessi
- Scrivere i test di integrazione con Testcontainers per verificare l'isolamento tra tenant

Risultato atteso: un utente autenticato può creare tenant, assegnare ruoli con permessi granulari, e il sistema impedisce l'accesso cross-tenant a livello di database.

---

### Fase 3 — Core Service: API key management e webhook

**Obiettivo:** il ciclo di vita completo delle API key e la notifica eventi verso sistemi esterni.

Attività:
- Implementare la generazione di API key: hash SHA-256 per lo storage (la chiave in chiaro viene mostrata una sola volta), prefisso leggibile per identificazione, scadenza configurabile
- Implementare la revoca e la rotazione delle API key
- Sviluppare il webhook system: ogni tenant può registrare URL di callback per eventi (key revocata, limite raggiunto, nuovo utente). Delivery asincrona con retry esponenziale e dead letter per webhook irraggiungibili
- Aggiungere le Flyway migration per le nuove tabelle
- Test di integrazione per il ciclo di vita delle key e il delivery dei webhook

Risultato atteso: un tenant può generare API key con scadenza e permessi specifici, revocarle, ruotarle, e ricevere notifiche su URL configurabili.

---

### Fase 4 — Gateway Service: pipeline reattiva

**Obiettivo:** il gateway non bloccante che processa ogni richiesta in ingresso.

Attività:
- Scaffolding del progetto Spring WebFlux
- Implementare la catena di filtri reattiva: ogni richiesta passa attraverso — estrazione API key dall'header → validazione contro Redis cache (o Core Service se cache miss) → verifica rate limit con sliding window su Redis → forwarding
- Implementare il rate limiting con sliding window in Lua script atomico su Redis (operazione atomica per evitare race condition)
- Implementare il circuit breaker con Resilience4j reactive verso il Core Service
- Propagare correttamente il security context nel reactive pipeline con `ReactiveSecurityContextHolder`
- Pubblicare l'evento di audit su Kafka al termine di ogni richiesta (fire-and-forget, non blocca la response)
- Test con richieste concorrenti per verificare il comportamento del rate limiter sotto carico

Risultato atteso: il gateway valida, limita e traccia ogni richiesta in modo completamente non bloccante. Il rate limiter è preciso anche con chiamate concorrenti.

---

### Fase 5 — Audit log: Kafka consumer e SSE streaming

**Obiettivo:** pipeline asincrona end-to-end dall'evento al client.

Attività:
- Implementare il Kafka consumer nel Core Service: legge gli eventi dal topic `audit-events`, li persiste su MySQL, gestisce i fallimenti con retry e dead letter topic
- Implementare l'endpoint SSE in Spring WebFlux che trasmette gli eventi di audit in tempo reale verso i client connessi
- Aggiungere filtri all'endpoint SSE: per tenant, per tipo di evento, per intervallo temporale
- Test del flusso completo: richiesta al gateway → evento Kafka → persistenza → streaming SSE

Risultato atteso: ogni richiesta al gateway genera un evento che, in pochi millisecondi, è visibile in streaming su un endpoint SSE, persistito su database, e consultabile con filtri.

---

### Fase 6 — CI/CD e production hardening

**Obiettivo:** pipeline automatizzata, osservabilità, e documentazione operativa.

Attività:
- Scrivere `ci.yml`: su ogni pull request — compilazione, test con Testcontainers, build delle immagini Docker, verifica che `docker-compose up` funzioni
- Scrivere `cd.yml`: su merge in main — push delle immagini su Docker Hub con tag basato su Git SHA
- Aggiungere health check endpoints (`/actuator/health`) con readiness e liveness probe separati su tutti i servizi Spring
- Configurare structured logging in JSON con Logback (pronto per essere ingerito da un sistema centralizzato)
- Aggiungere i Dockerfile multi-stage ottimizzati per ridurre la dimensione delle immagini di produzione
- Scrivere gli Architecture Decision Records per le scelte tecniche principali
- Scrivere il README professionale con: descrizione del progetto, architettura con diagramma, prerequisiti, istruzioni di setup, variabili di configurazione
- Generare la documentazione OpenAPI con SpringDoc per tutti gli endpoint REST

Risultato atteso: un nuovo contributor può clonare il repository, eseguire `docker-compose up`, e avere l'intero stack funzionante. Ogni PR è validata automaticamente. La documentazione spiega il perché oltre al come.

---

### Fase 7 — Frontend (Next.js 16)

**Obiettivo:** interfaccia di amministrazione completa e professionale.

Attività:
- Setup Next.js 16 con App Router, TypeScript, Tailwind CSS
- Implementare il flusso di autenticazione con Keycloak (PKCE): login, refresh automatico del token, logout, protezione delle route
- Dashboard principale: metriche aggregate (request rate, error rate, latenza media) con grafici basati su dati storici da Core Service
- Audit log viewer: tabella paginata con filtri per tenant, tipo evento, intervallo temporale, e toggle per attivare lo streaming SSE in tempo reale
- Tenant management: lista tenant, creazione, configurazione limiti di rate
- RBAC editor: visualizzazione ad albero di ruoli e permessi, assegnazione a utenti
- API key management: generazione (con visualizzazione one-time della chiave), lista, revoca, rotazione
- Containerizzazione: Dockerfile multi-stage con build Next.js e serving tramite Nginx

Risultato atteso: interfaccia completa che copre tutti i casi d'uso della piattaforma, con autenticazione reale via Keycloak e comunicazione con entrambi i servizi backend.

---

## Cosa dimostra questo progetto

| Area | Evidenza concreta |
|---|---|
| Sistemi distribuiti | Rate limiting distribuito su Redis, Kafka per disaccoppiamento, circuit breaker |
| Security | Keycloak PKCE, RBAC custom su Spring Security 6, tenant isolation a livello DB |
| Reactive programming | Gateway Spring WebFlux, SSE streaming, reactive security context propagation |
| Testing | Testcontainers con infrastruttura reale in ogni test run |
| CI/CD | GitHub Actions con pipeline completa, immagini versionati su Docker Hub |
| Architecture thinking | ADR documentati, separazione netta delle responsabilità tra i servizi |
| Operational maturity | Structured logging, health probes, OpenAPI docs, README professionale |
