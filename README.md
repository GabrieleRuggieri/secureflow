# SecureFlow — Multi-Tenant Authorization Platform

Piattaforma self-hosted per la gestione centralizzata di autenticazione, autorizzazione e rate limiting delle API.

## Struttura del progetto

```
secureflow/
├── backend/           # Servizi Java
│   ├── gateway-service/   # Spring WebFlux - API gateway
│   └── core-service/      # Spring Boot - CRUD, RBAC, API key
├── frontend/          # Next.js 16 - Dashboard admin
├── devops/            # Infrastruttura e configurazione
│   ├── mysql/        # Init script database
│   ├── nginx/         # Configurazione reverse proxy
│   ├── keycloak/      # Realm export
│   └── flyway/        # Migration database
├── docs/              # Documentazione e ADR
└── docker-compose.yml
```

## Avvio rapido

```bash
# Copia le variabili d'ambiente (nella root del progetto)
cp devops/.env.example .env

# Avvia lo stack (MySQL, Redis, Kafka, Keycloak, Nginx)
docker compose up -d --build

# (Opzionale) Migration Flyway + topic Kafka
docker compose --profile init up -d
```

## Servizi

| Servizio | Porta | Descrizione |
|----------|-------|-------------|
| MySQL 8.4 | 3306 | Database (secureflow + keycloak) |
| Redis 8 | 6379 | Cache e rate limiting |
| Kafka 4.x (KRaft) | 9092 | Messaging per audit events |
| Keycloak 26 | 8180 | Identity Provider |
| Nginx | 80, 443 | Reverse proxy |

## Keycloak

- **Admin Console**: http://localhost/auth/admin (admin / admin)
- **Realm**: secureflow
- **Client frontend**: secureflow-frontend (PKCE)
- **Client Core Service**: secureflow-core (client credentials)

## Verifica connettività

```bash
docker exec secureflow-mysql mysql -u secureflow -psecureflow -e "SHOW DATABASES;"
docker exec secureflow-redis redis-cli ping
docker exec secureflow-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
curl -s http://localhost/auth/health/ready
```
