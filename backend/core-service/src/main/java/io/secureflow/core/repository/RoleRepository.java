/*
 * RoleRepository — Accesso dati per Role.
 *
 * findByTenantId: lista ruoli del tenant (filtro applicato). findByTenantIdAndName: check
 * univocità in create. findByIdWithPermissions: fetch join per evitare N+1 in update/display.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByTenantId(UUID tenantId);

    Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Fetch join per Permission: una query invece di N+1. Quando toDto accede a
     * role.getPermissions().stream().map(Permission::getId), i Permission sono già in memoria.
     */
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(UUID id);
}
