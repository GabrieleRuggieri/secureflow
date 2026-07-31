/*
 * UserService — Logica di business per la gestione utenti.
 *
 * Usa TenantContext per il tenant corrente (impostato da TenantContextFilter dal JWT).
 * Validazione: keycloak_user_id univoco per tenant. Gestisce l'associazione User-Role
 * tramite RoleAssignment: in create/update si sostituisce l'intero set di ruoli.
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.UserDto;
import io.secureflow.core.entity.Role;
import io.secureflow.core.entity.RoleAssignment;
import io.secureflow.core.entity.User;
import io.secureflow.core.repository.RoleRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.UserRepository;
import io.secureflow.core.security.TenantContext;
import io.secureflow.core.webhook.WebhookDispatcher;
import io.secureflow.core.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final WebhookDispatcher webhookDispatcher;

    /**
     * Elenca gli utenti del tenant corrente. TenantContext.getTenantId() dal JWT (filter).
     * findAll() è filtrato da Hibernate: restituisce solo user con tenant_id = current.
     */
    @Transactional(readOnly = true)
    public List<UserDto> list() {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Recupera user con ruoli e permessi (fetch join). Necessario per toDto che accede a
     * roleAssignments -> role -> permissions. Senza fetch: N+1 query (1 per user, N per ruoli).
     */
    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        return userRepository.findByIdWithRolesAndPermissions(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Crea un nuovo user. Validazione: keycloak_user_id univoco per tenant (stesso utente
     * Keycloak può essere in tenant diversi). Se roleIds presente, crea RoleAssignment per
     * ogni ruolo: nuova RoleAssignment, setUser/setRole, add alla collection. orphanRemoval
     * + cascade: il save propaga le RoleAssignment. Due save: prima l'user (per avere id),
     * poi con le RoleAssignment (che usano user.getId() nell'EmbeddedId).
     */
    @Transactional
    public UserDto create(UserDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        var tenant = tenantRepository.findById(tenantId).orElseThrow();

        if (userRepository.findByTenantIdAndKeycloakUserId(tenantId, request.keycloakUserId()).isPresent()) {
            throw new IllegalArgumentException("User already exists for Keycloak ID: " + request.keycloakUserId());
        }

        User user = new User();
        user.setTenant(tenant);
        user.setKeycloakUserId(request.keycloakUserId());
        user.setEmail(request.email());
        user.setUsername(request.username());
        user = userRepository.save(user);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            for (Role role : roles) {
                RoleAssignment ra = new RoleAssignment();
                ra.setUser(user);
                ra.setRole(role);
                user.getRoleAssignments().add(ra);
            }
            user = userRepository.save(user);
        }

        webhookDispatcher.dispatch(tenantId, WebhookEventType.USER_CREATED, Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "username", user.getUsername()
        ));
        return toDto(user);
    }

    /**
     * Aggiorna i ruoli di un user. clear() sulla collection + orphanRemoval = DELETE delle
     * vecchie RoleAssignment. Poi crea le nuove. Replace completo: non merge, sostituzione.
     */
    @Transactional
    public UserDto update(UUID id, UserDto.Update request) {
        return userRepository.findByIdWithRolesAndPermissions(id)
                .map(user -> {
                    if (request.roleIds() != null) {
                        user.getRoleAssignments().clear();
                        if (!request.roleIds().isEmpty()) {
                            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
                            for (Role role : roles) {
                                RoleAssignment ra = new RoleAssignment();
                                ra.setUser(user);
                                ra.setRole(role);
                                user.getRoleAssignments().add(ra);
                            }
                        }
                    }
                    return userRepository.save(user);
                })
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    /**
     * Converte User in DTO. Estrae roleIds dalla collection roleAssignments. Richiede che
     * User sia stato caricato con findByIdWithRolesAndPermissions (fetch join), altrimenti
     * LazyInitializationException accedendo a getRole().
     */
    private UserDto toDto(User u) {
        Set<UUID> roleIds = u.getRoleAssignments().stream()
                .map(ra -> ra.getRole().getId())
                .collect(Collectors.toSet());
        return new UserDto(u.getId(), u.getKeycloakUserId(), u.getEmail(), u.getUsername(), roleIds);
    }
}
