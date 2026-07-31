# ADR 001 — Spring WebFlux for the API Gateway
#
# Status: Accepted
# Date: 2026-03-10

## Context

SecureFlow's gateway must authenticate API keys, enforce distributed rate limits,
proxy upstream traffic, and publish audit events for every request. Under load it
will handle many concurrent connections that spend most of their time waiting on
I/O (Redis, Core Service, Kafka, upstream HTTP).

## Options

1. Spring MVC (servlet, thread-per-request)
2. Spring WebFlux (reactive, non-blocking event loop)
3. A dedicated proxy (Kong, Envoy) with custom plugins

## Decision

Use **Spring WebFlux** for `gateway-service`.

## Consequences

- High concurrency with a small thread pool; natural fit for Redis reactive and WebClient
- Security context must be propagated via `ReactiveSecurityContextHolder`
- Team must be comfortable with Reactor operators; Core Service stays imperative MVC
- Avoids introducing an external gateway product for a portfolio/self-hosted project
