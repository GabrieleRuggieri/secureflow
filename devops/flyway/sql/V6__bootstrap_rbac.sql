-- V6 — Bootstrap RBAC for the default tenant (dashboard first login).
-- Fixed UUIDs keep seeds idempotent. JwtUserBootstrap links the Keycloak user at runtime.

SET @tenant := UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', ''));
SET @role_admin := UNHEX(REPLACE('00000000-0000-0000-0000-000000000010', '-', ''));

INSERT IGNORE INTO role (id, tenant_id, name, description, created_at, updated_at)
VALUES (@role_admin, @tenant, 'admin', 'Full dashboard access', NOW(), NOW());

-- permission ids 100+
INSERT IGNORE INTO permission (id, tenant_id, resource, action, description, created_at, updated_at) VALUES
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000101', '-', '')), @tenant, 'tenant', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000102', '-', '')), @tenant, 'tenant', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000103', '-', '')), @tenant, 'tenant', 'update', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000104', '-', '')), @tenant, 'tenant', 'delete', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000111', '-', '')), @tenant, 'user', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000112', '-', '')), @tenant, 'user', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000113', '-', '')), @tenant, 'user', 'update', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000114', '-', '')), @tenant, 'user', 'delete', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000121', '-', '')), @tenant, 'role', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000122', '-', '')), @tenant, 'role', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000123', '-', '')), @tenant, 'role', 'update', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000124', '-', '')), @tenant, 'role', 'delete', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000131', '-', '')), @tenant, 'permission', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000132', '-', '')), @tenant, 'permission', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000133', '-', '')), @tenant, 'permission', 'update', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000134', '-', '')), @tenant, 'permission', 'delete', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000141', '-', '')), @tenant, 'apikey', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000142', '-', '')), @tenant, 'apikey', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000143', '-', '')), @tenant, 'apikey', 'revoke', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000144', '-', '')), @tenant, 'apikey', 'rotate', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000151', '-', '')), @tenant, 'webhook', 'read', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000152', '-', '')), @tenant, 'webhook', 'create', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000153', '-', '')), @tenant, 'webhook', 'update', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000154', '-', '')), @tenant, 'webhook', 'delete', NULL, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000161', '-', '')), @tenant, 'audit', 'read', NULL, NOW(), NOW());

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT @role_admin, id FROM permission WHERE tenant_id = @tenant;
