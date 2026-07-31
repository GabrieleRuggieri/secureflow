/*
 * JwtUserBootstrapService — Al primo accesso dashboard crea User + ruolo admin se manca.
 *
 * Il JWT Keycloak è valido, ma Core richiede una riga in `user` con i permessi RBAC.
 * Senza questo step ogni API risponde 403 dopo il login.
 */
package io.secureflow.core.security;

import io.secureflow.core.entity.Role;
import io.secureflow.core.entity.RoleAssignment;
import io.secureflow.core.entity.RoleAssignmentId;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.User;
import io.secureflow.core.repository.RoleRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtUserBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(JwtUserBootstrapService.class);
    private static final String ADMIN_ROLE = "admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public JwtUserBootstrapService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void ensureUser(Jwt jwt, UUID tenantId) {
        String keycloakUserId = jwt.getSubject();
        if (keycloakUserId == null || keycloakUserId.isBlank() || tenantId == null) {
            return;
        }

        Role adminRole = roleRepository.findByTenantIdAndName(tenantId, ADMIN_ROLE).orElse(null);
        if (adminRole == null) {
            log.warn("JWT bootstrap skipped: role '{}' missing for tenant {}", ADMIN_ROLE, tenantId);
            return;
        }

        var existing = userRepository.findByTenantIdAndKeycloakUserIdWithRolesAndPermissions(tenantId, keycloakUserId);
        if (existing.isPresent()) {
            User user = existing.get();
            boolean hasAdmin = user.getRoleAssignments().stream()
                    .anyMatch(ra -> ADMIN_ROLE.equals(ra.getRole().getName()));
            if (!hasAdmin) {
                assignAdmin(user, adminRole);
                userRepository.save(user);
                log.info("Assigned admin role to existing user {} ({})", user.getUsername(), keycloakUserId);
            }
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("JWT bootstrap skipped: tenant {} not found", tenantId);
            return;
        }

        String email = firstNonBlank(jwt.getClaimAsString("email"), keycloakUserId + "@secureflow.local");
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                keycloakUserId);

        User user = new User();
        user.setTenant(tenant);
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(email);
        user.setUsername(username);
        user = userRepository.save(user);

        assignAdmin(user, adminRole);
        userRepository.save(user);

        log.info("Bootstrapped dashboard user {} ({}) for tenant {}", username, keycloakUserId, tenantId);
    }

    private static void assignAdmin(User user, Role adminRole) {
        RoleAssignment assignment = new RoleAssignment();
        assignment.setId(new RoleAssignmentId(user.getId(), adminRole.getId()));
        assignment.setUser(user);
        assignment.setRole(adminRole);
        assignment.setAssignedAt(Instant.now());
        user.getRoleAssignments().add(assignment);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
