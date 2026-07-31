-- V5 — Audit event table (Fase 5)
--
-- Persiste gli eventi pubblicati dal gateway su Kafka topic audit-events.
-- tenant_id + occurred_at + outcome sono le colonne di filtro principali per
-- lista paginata e SSE. event_id è l'id generato dal gateway (idempotenza).
CREATE TABLE audit_event (
    id BINARY(16) NOT NULL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    key_prefix VARCHAR(20),
    method VARCHAR(16) NOT NULL,
    path VARCHAR(512) NOT NULL,
    status INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_audit_event_id (event_id),
    KEY idx_audit_tenant_occurred (tenant_id, occurred_at),
    KEY idx_audit_tenant_outcome (tenant_id, outcome),
    CONSTRAINT fk_audit_event_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
