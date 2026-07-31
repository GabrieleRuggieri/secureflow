# SecureFlow — Multi-Tenant Authorization Platform

Piattaforma **self-hosted** per autenticazione API (API key), autorizzazione RBAC, rate limiting distribuito e audit log in tempo reale — eseguibile interamente in locale con Docker.

## Cos'è

SecureFlow centralizza tre domande tipiche di chi espone API a clienti esterni:

- **Chi sei?** — autenticazione tramite API key
- **Cosa puoi fare?** — autorizzazione basata su ruoli e permessi (RBAC) nella console di gestione
- **Quanto puoi farlo?** — rate limiting per tenant

È paragonabile a un pannello tipo Stripe (ciclo di vita delle API key) combinato con rate limit in stile Cloudflare, senza costi di licenza e senza lock-in cloud.

> **Nota:** Keycloak autentica gli utenti della **dashboard** (OIDC/PKCE). Il traffico API verso il gateway usa **API key**, non il login OIDC.

## Valore tecnico

- Concorrenza e stato condiviso (rate limiting distribuito su Redis)
- Security architecture (RBAC, isolamento tenant a livello DB)
- Reactive programming (gateway Spring WebFlux)
- Event-driven design (audit asincrono via Kafka)
- Operational maturity (CI/CD, health checks, structured logging, ADR)

## Architettura

```text
Clients → Nginx → Keycloak (/auth) | Gateway (/gw) | Core (/core)
                      │                  │               │
                   MySQL              Redis/Kafka      MySQL/Kafka
```

Dettagli: [docs/architecture.md](docs/architecture.md) · Decisioni: [docs/adr/](docs/adr/)

| Servizio | Stack | Responsabilità |
|----------|-------|----------------|
| **gateway-service** | Spring WebFlux | Auth API key, rate limit sliding-window Redis, proxy, publish audit su Kafka |
| **core-service** | Spring Boot MVC | Tenant, RBAC, API key, webhook, consumer audit + SSE |
| **Keycloak** | OIDC | Login dashboard (PKCE); non usato per le API key |
| **MySQL / Redis / Kafka** | — | Persistenza, cache/limiti, audit asincrono |
| **frontend** | Next.js | Console admin (metriche, key, audit, tenant, RBAC in lettura) |
| **Nginx** | — | Origin unico `http://localhost` (UI, IdP, API) |

## Stack

| Area | Tecnologia |
|------|------------|
| Gateway | Spring WebFlux |
| Core | Spring Boot 3 (MVC), Flyway, Hibernate tenant filter |
| IdP dashboard | Keycloak ≥ 26.0.1 (cookie HTTP localhost / Safari) |
| DB | MySQL 8.4 |
| Cache / rate limit | Redis (sliding window Lua) |
| Messaging | Kafka (KRaft) |
| Frontend | Next.js (App Router), TanStack Query |
| CI/CD | GitHub Actions (test + build immagini; artifact, non Docker Hub) |

## Quick start

```bash
cp .env.example .env

# Infrastruttura
docker compose up -d

# Schema + topic Kafka
docker compose --profile init up -d

# Core + Gateway + Frontend (container)
docker compose --profile apps up -d --build
```

Apri **http://localhost** — login `admin` / `admin` (client PKCE `secureflow-frontend`).

App su host contro l’infra Compose:

```bash
cd backend/core-service && mvn spring-boot:run
cd backend/gateway-service && mvn spring-boot:run
```

Frontend in host-dev:

```bash
docker compose -f docker-compose.yml -f docker-compose.host-dev.yml up -d
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

## Endpoint

Origin pubblica: **http://localhost** (nginx). UI, Keycloak (`/auth`) e API condividono schema/host/porta.

| URL | Descrizione |
|-----|-------------|
| http://localhost | Dashboard |
| http://localhost/auth/admin | Keycloak admin (`admin` / `admin`) |
| http://localhost/core/api/... | Core API |
| http://localhost/gw/... | Gateway proxy |
| http://localhost:8081/actuator/health | Health Core (ops) |
| http://localhost:8080/actuator/health | Health Gateway (ops) |

## Uso tipico (API key)

1. In dashboard → **API key** → genera e copia la chiave (visibile una sola volta).
2. Chiama il gateway:

```bash
curl -i -H "X-API-Key: INCOLLA_LA_CHIAVE" \
  http://localhost/gw/actuator/health
```

3. Controlla **Audit** / **Dashboard**: successi, denied (401), rate_limited, latenza.

Senza chiave o con chiave invalida → `401` registrato come `denied`.

## Porte

| Servizio | Porta host |
|----------|------------|
| Nginx (UI + IdP + API) | 80 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 19092 (host) / 9092 (rete Docker) |
| Keycloak | 8180 (ops; preferire `/auth`) |
| Gateway | 8080 (ops; preferire `/gw`) |
| Core | 8081 (ops; preferire `/core`) |
| Frontend | solo interno (via nginx) |

## Configurazione

Copia `.env.example` → `.env`.

| Variabile | Default | Scopo |
|-----------|---------|--------|
| `MYSQL_*` | secureflow | Credenziali DB |
| `KEYCLOAK_ADMIN*` | admin | Admin bootstrap Keycloak |
| `SECUREFLOW_INTERNAL_TOKEN` | secureflow-internal | Header M2M Gateway ↔ Core |

Env app (host o profile `apps`):

| Variabile | Esempio |
|-----------|---------|
| `KAFKA_BOOTSTRAP` | `localhost:19092` (host) / `kafka:9092` (Docker) |
| `JWK_SET_URI` | URL JWKS Keycloak |
| `SPRING_PROFILES_ACTIVE` | `docker` nei container |

Log JSON strutturati: profile Spring `json-logs` o `docker`.

## Cosa dimostra

| Area | Evidenza |
|------|----------|
| Sistemi distribuiti | Rate limit Redis, Kafka per disaccoppiamento, circuit breaker |
| Security | Keycloak PKCE, RBAC Spring Security 6, tenant isolation DB |
| Reactive | Gateway WebFlux, SSE audit, context reattivo |
| Testing | Testcontainers (MySQL/Redis/Kafka reali nei test) |
| CI/CD | GitHub Actions: test, build immagini, artifact self-hosted |
| Architecture | ADR, separazione Gateway / Core / IdP |
| Ops | Structured logging, health probes, OpenAPI |

## Fasi implementate

1. Infrastruttura locale (Compose, Keycloak realm, Flyway, Nginx)
2. Core: modello dati, tenant isolation, RBAC
3. API key lifecycle e webhook
4. Gateway reattivo (auth, rate limit, proxy, audit publish)
5. Audit: consumer Kafka + SSE
6. CI/CD, hardening, ADR, OpenAPI
7. Frontend Next.js (dashboard, key, audit, tenant, RBAC)

## Workflow di sviluppo

```text
main (stabile) ← develop ← feature/* | fix/*  (PR per cambio)
```

```bash
git checkout develop && git pull
git checkout -b feature/my-change
# ... commit, push, PR su develop
```

CI (`.github/workflows/ci.yml`) su ogni PR: test Maven + build immagini + smoke Compose/Flyway.

CD (`.github/workflows/cd.yml`): build immagini e upload come **artifact** GitHub Actions (nessun Docker Hub).

## Test

```bash
cd backend/core-service && mvn test
cd backend/gateway-service && mvn test

# Integrazione (serve Docker)
mvn test -Dsurefire.excludedGroups= -Dgroups=integration
```

## Verify

```bash
docker exec secureflow-mysql mysqladmin ping -h localhost
docker exec secureflow-redis redis-cli ping
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8080/actuator/health
curl -s http://localhost/auth/health/ready
```

## Licenza / costo

Progetto educational / portfolio — stack **€0**, tutto open source e containerizzato.
