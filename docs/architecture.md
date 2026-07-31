# Architecture

SecureFlow is a self-hosted multi-tenant API authorization platform.

```text
                    ┌─────────────┐
   Clients ───────► │    Nginx    │
                    └──────┬──────┘
           /auth           │ /gw  /core
            │              │
            ▼              ▼
      ┌──────────┐   ┌─────────────┐       ┌──────────────┐
      │ Keycloak │   │   Gateway   │──────►│ Core Service │
      └──────────┘   │  (WebFlux)  │       │   (MVC)      │
                     └──────┬──────┘       └──────┬───────┘
                            │                     │
                     Redis  │              MySQL  │
                     Kafka ─┴─────────────────────┘
```

| Component | Role |
|-----------|------|
| Nginx | TLS termination / path routing |
| Keycloak | Dashboard IdP (OIDC/PKCE) |
| Gateway | API key auth, rate limit, proxy, audit publish |
| Core Service | Tenants, RBAC, API keys, webhooks, audit consume/SSE |
| Redis | API key cache + sliding-window rate limit |
| Kafka | Async audit pipeline |
| MySQL | System of record |

See [ADR index](adr/) for decision records.
