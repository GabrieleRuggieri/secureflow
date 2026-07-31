/*
 * UserRepository — Accesso dati per User.
 *
 * findByTenantIdAndKeycloakUserId: lookup per RbacPermissionEvaluator (JWT sub + tenant).
 * findByIdWithRolesAndPermissions: fetch join per evitare N+1 quando si caricano ruoli e
 * permessi per la valutazione RBAC. Il filtro tenant è applicato automaticamente.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN FETCH u.roleAssignments ra JOIN FETCH ra.role r JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(UUID id);

    Optional<User> findByTenantIdAndKeycloakUserId(UUID tenantId, String keycloakUserId);

    /**
     * Fetch join: una query carica User + RoleAssignment + Role + Permission. Evita N+1
     * quando si iterano i permessi (RbacPermissionEvaluator). tenantId + keycloakUserId
     * identificano univocamente l'utente.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roleAssignments ra LEFT JOIN FETCH ra.role r LEFT JOIN FETCH r.permissions WHERE u.tenant.id = :tenantId AND u.keycloakUserId = :keycloakUserId")
    Optional<User> findByTenantIdAndKeycloakUserIdWithRolesAndPermissions(UUID tenantId, String keycloakUserId);
}
