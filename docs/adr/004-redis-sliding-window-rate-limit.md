# ADR 004 — Redis sliding-window rate limit
#
# Status: Accepted
# Date: 2026-03-10

## Context

Rate limits must be correct across multiple gateway instances. An in-memory
counter per process can be bypassed by spreading traffic.

## Options

1. Fixed-window counter in Redis
2. Sliding-window log / counter in Redis (Lua)
3. Token bucket in Redis

## Decision

Use a **sliding window** implemented as an atomic Redis Lua script.

## Consequences

- Shared state across replicas; no burst at window boundaries like fixed windows
- Lua keeps check-and-increment atomic under concurrency
- Redis becomes critical for the hot path (health/readiness should reflect this)
- Slightly more complex than INCR+EXPIRE, worth it for fairness
