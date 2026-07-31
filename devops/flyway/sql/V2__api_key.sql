-- V2 — Tabella api_key
--
-- Struttura per le API key (gestione completa in Fase 3). key_hash: SHA-256, mai in chiaro.
-- key_prefix: identificatore leggibile (es. sf_live_xxx). expires_at, revoked_at per
-- scadenza e revoca. created_by: utente che ha creato la chiave.
CREATE TABLE api_key (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    expires_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BINARY(16),
    UNIQUE KEY uk_api_key_tenant_prefix (tenant_id, key_prefix),
    CONSTRAINT fk_api_key_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_api_key_creator FOREIGN KEY (created_by) REFERENCES `user`(id) ON DELETE SET NULL
);
