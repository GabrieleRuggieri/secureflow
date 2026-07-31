/*
 * PermissionRepository — Accesso dati per Permission.
 *
 * findByTenantIdAndResourceAndAction: check univocità (resource, action) per tenant in create.
 * Il filtro tenant garantisce che findAll e findById restituiscano solo permessi del tenant
 * corrente.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findByTenantId(UUID tenantId);

    Optional<Permission> findByTenantIdAndResourceAndAction(UUID tenantId, String resource, String action);
}
