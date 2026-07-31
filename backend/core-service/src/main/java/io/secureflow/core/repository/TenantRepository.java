/*
 * TenantRepository — Accesso dati per Tenant.
 *
 * Tenant non ha il filtro tenant (è entità root). findBySlug usato da TenantContextFilter
 * per risolvere "default-tenant" e altri slug in UUID. existsBySlug per validazione in create.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
