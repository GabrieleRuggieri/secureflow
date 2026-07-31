# ADR 003 — Kafka for audit events
#
# Status: Accepted
# Date: 2026-03-10

## Context

Every gateway request should leave an audit trail. Writing synchronously to MySQL
from the gateway would add latency and couple the hot path to the database.

## Options

1. Synchronous JDBC insert from the gateway
2. Async local queue in-process
3. Kafka topic consumed by Core Service

## Decision

Publish **fire-and-forget** audit events to Kafka (`audit-events`). Core Service
consumes, persists, and fans out to SSE. Failed processing goes to `audit-events.DLT`.

## Consequences

- Gateway stays non-blocking; Core can scale consumers independently
- At-least-once delivery → idempotent persist on `event_id`
- KRaft mode (no ZooKeeper) keeps local compose simpler
- Extra moving part versus a DB write, justified by decoupling and resilience
