/*
 * TenantContext — Thread-local per il tenant della richiesta corrente.
 *
 * Impostato da TenantContextFilter dal claim "tenantId" del JWT (UUID o slug). Usato da
 * TenantFilterEntityManagerFactory per abilitare il filtro Hibernate. clear() in finally
 * del filter per evitare leak tra richieste (thread pool).
 */
package io.secureflow.core.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the current tenant ID, extracted from the JWT.
 * Used by the Hibernate tenant filter to isolate queries.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(TENANT_ID.get());
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
