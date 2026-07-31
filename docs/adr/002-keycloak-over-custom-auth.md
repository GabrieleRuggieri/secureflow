# ADR 002 — Keycloak over custom authentication
#
# Status: Accepted
# Date: 2026-03-10

## Context

Dashboard users need login, logout, token refresh, and tenant claims in JWTs.
Building password storage, MFA, and OIDC correctly is expensive and risky.

## Options

1. Custom auth (Spring Security form login + self-issued JWT)
2. Keycloak (self-hosted IdP)
3. Hosted IdP (Auth0, Cognito) — conflicts with fully local Docker goal

## Decision

Use **Keycloak** for dashboard identity only. API keys remain owned by Core Service.

## Consequences

- PKCE for the Next.js frontend; client-credentials for M2M where needed
- Realm export (`devops/keycloak/realm-export.json`) keeps setup reproducible
- JWT `tenantId` claim drives Hibernate tenant isolation
- Operational cost of running Keycloak in compose (acceptable for self-hosted stack)
