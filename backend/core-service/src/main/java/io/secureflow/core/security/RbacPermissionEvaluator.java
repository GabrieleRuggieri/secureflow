/*
 * RbacPermissionEvaluator — Verifica permessi per Spring Security.
 *
 * Implementa PermissionEvaluator: hasPermission(authentication, target, "resource:action").
 * Carica l'User dal DB (tenant + keycloak sub dal JWT) e verifica che abbia un Role con
 * il Permission richiesto. Fallback: se l'utente ha ruolo realm "admin" in Keycloak,
 * ha tutti i permessi (bootstrap senza User in DB).
 */
package io.secureflow.core.security;

import io.secureflow.core.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

@Component
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private final UserRepository userRepository;

    public RbacPermissionEvaluator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Implementazione PermissionEvaluator per hasPermission(target, permission). Spring
     * invoca questo metodo quando trova @PreAuthorize("hasPermission(null, 'tenant:create')").
     * Ignoriamo target: il permesso è globale, non su un oggetto specifico.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) return false;
        return hasPermission(authentication, permission.toString());
    }

    /** Variante con targetId e targetType. Stesso comportamento: deleghiamo alla logica centrale. */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || permission == null) return false;
        return hasPermission(authentication, permission.toString());
    }

    /**
     * Logica centrale: verifica se l'utente ha il permesso "resource:action".
     * 1) Estrae sub (keycloak_user_id) e tenantId dal JWT/TenantContext.
     * 2) Fallback admin: se il JWT ha realm_access.roles contenente "admin", permesso OK.
     *    Serve per bootstrap: l'admin Keycloak può accedere prima che esista User in DB.
     * 3) Altrimenti: carica User con Role e Permission (fetch join). Per ogni RoleAssignment
     *    → Role → Permission, controlla se resource:action coincide o è "*:*" (superuser).
     */
    private boolean hasPermission(Authentication authentication, String requiredPermission) {
        if (!requiredPermission.contains(":")) return false;

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) return false;

        String keycloakUserId = jwt.getSubject();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return false;

        if (jwt.getClaimAsString("realm_access") != null) {
            var realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map<?, ?> map) {
                var roles = map.get("roles");
                if (roles instanceof java.util.List<?> roleList && roleList.contains("admin")) {
                    return true;
                }
            }
        }

        return userRepository.findByTenantIdAndKeycloakUserIdWithRolesAndPermissions(tenantId, keycloakUserId)
                .map(user -> user.getRoleAssignments().stream()
                        .flatMap(ra -> ra.getRole().getPermissions().stream())
                        .map(p -> p.getResource() + ":" + p.getAction())
                        .anyMatch(p -> p.equals(requiredPermission) || p.equals("*:*")))
                .orElse(false);
    }
}
