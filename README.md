# SecureFlow — Multi-Tenant Authorization Platform

Self-hosted platform for API authentication, RBAC authorization, and distributed rate limiting — with a live audit trail.

Comparable to a Stripe-style API key console plus Cloudflare-like rate limits, runnable entirely on your laptop with Docker.

## Architecture

```text
Clients → Nginx → Keycloak (/auth) | Gateway (/gw) | Core (/core)
                      │                  │               │
                   MySQL              Redis/Kafka      MySQL/Kafka
```

Details: [docs/architecture.md](docs/architecture.md) · Decisions: [docs/adr/](docs/adr/)

| Service | Stack | Responsibility |
|---------|-------|----------------|
| **gateway-service** | Spring WebFlux | API key auth, Redis sliding-window rate limit, proxy, Kafka audit publish |
| **core-service** | Spring Boot MVC | Tenants, RBAC, API keys, webhooks, audit consume + SSE |
| **Keycloak** | OIDC | Dashboard login (PKCE); not used for API keys |
| **MySQL / Redis / Kafka** | — | Persistence, cache/limits, async audit |

## Prerequisites

- Docker Desktop (or Engine + Compose v2)
- JDK 21 + Maven 3.9+ (only if you run apps on the host)
- Optional: `gh` for PRs

## Quick start (all local)

```bash
cp .env.example .env

# Infrastructure
docker compose up -d

# Schema + Kafka topics
docker compose --profile init up -d

# Optional: build & run Core + Gateway as containers
docker compose --profile apps up -d --build
```

Or run apps on the host against compose infra:

```bash
cd backend/core-service && mvn spring-boot:run
cd backend/gateway-service && mvn spring-boot:run
```

## Endpoints

| URL | Description |
|-----|-------------|
| http://localhost:3000 | Dashboard Next.js (IT/EN toggle) |
| http://localhost/auth/admin | Keycloak admin (`admin` / `admin`) |
| http://localhost:8081/actuator/health | Core liveness/readiness |
| http://localhost:8080/actuator/health | Gateway health |
| http://localhost:8081/swagger-ui.html | Core OpenAPI UI |
| http://localhost:8080/swagger-ui.html | Gateway OpenAPI UI |
| http://localhost/core/api/... | Core API via Nginx |
| http://localhost/gw/... | Gateway proxy via Nginx |

### Frontend (host)

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Sign-in uses Keycloak PKCE (`secureflow-frontend`).

## Ports

| Service | Host port |
|---------|-----------|
| Nginx | 80, 443 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 19092 (host apps) / 9092 (docker network) |
| Keycloak | 8180 |
| Gateway | 8080 |
| Core | 8081 |

## Configuration

Copy `.env.example` → `.env`. Important variables:

| Variable | Default | Purpose |
|----------|---------|---------|
| `MYSQL_*` | secureflow | Database credentials |
| `KEYCLOAK_ADMIN*` | admin | Keycloak bootstrap admin |
| `SECUREFLOW_INTERNAL_TOKEN` | secureflow-internal | Gateway ↔ Core M2M header |

App env (host or compose profile `apps`):

| Variable | Example |
|----------|---------|
| `KAFKA_BOOTSTRAP` | `localhost:19092` (host) / `kafka:9092` (docker profile) |
| `JWK_SET_URI` | Keycloak JWKS URL |
| `SPRING_PROFILES_ACTIVE` | `docker` inside compose |

Structured JSON logs: activate Spring profile `json-logs` or `docker`.

## Development workflow

```text
main (stable) ← develop ← feature/* | fix/*  (PR per phase)
```

```bash
git checkout develop && git pull
git checkout -b feature/my-change
# ... commit, push, PR into develop
```

CI (`.github/workflows/ci.yml`) on every PR: Maven unit tests + Docker image builds + compose/Flyway smoke.

CD (`.github/workflows/cd.yml`) builds images and uploads them as **GitHub Actions artifacts** (no Docker Hub — fully self-hosted).

## Tests

```bash
# Unit tests (default; integration group excluded)
cd backend/core-service && mvn test
cd backend/gateway-service && mvn test

# Integration (Docker required)
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

## License

Educational / portfolio project — €0 stack, all open source.
