-- V3 — Tenant di default per bootstrap
--
-- L'admin Keycloak ha tenantId "default-tenant" (attributo utente). Questo tenant con
-- UUID fisso permette al TenantContextFilter di risolvere lo slug in UUID senza
-- creare il tenant a mano. INSERT IGNORE per idempotenza su re-run.
INSERT IGNORE INTO tenant (id, name, slug, rate_limit_per_minute, created_at, updated_at)
VALUES (UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'Default Tenant', 'default-tenant', 60, NOW(), NOW());
