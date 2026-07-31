/*
 * RoleService — Logica di business per la gestione ruoli.
 *
 * Validazione: nome univoco per tenant. La relazione Role-Permission è ManyToMany: in
 * create/update si caricano i Permission per ID e si assegnano al ruolo. Il filtro
 * tenant garantisce che si vedano solo ruoli e permessi del tenant corrente.
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.RoleDto;
import io.secureflow.core.entity.Permission;
import io.secureflow.core.entity.Role;
import io.secureflow.core.repository.PermissionRepository;
import io.secureflow.core.repository.RoleRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;

    /**
     * Elenca i ruoli del tenant. findByTenantId usa il filtro; in alternativa findAll()
     * sarebbe già filtrato. Esplicito per chiarezza.
     */
    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        return roleRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Recupera ruolo con fetch join per Permission. Evita N+1 quando toDto accede a
     * role.getPermissions().stream().map(Permission::getId).
     */
    @Transactional(readOnly = true)
    public RoleDto get(UUID id) {
        return roleRepository.findByIdWithPermissions(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Crea un ruolo. Validazione: nome univoco per tenant. Se permissionIds presente,
     * findAllById carica i Permission (devono essere dello stesso tenant per il filtro)
     * e li assegna al ruolo. La ManyToMany è gestita da Hibernate: la tabella role_permission
     * viene popolata automaticamente.
     */
    @Transactional
    public RoleDto create(RoleDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        var tenant = tenantRepository.findById(tenantId).orElseThrow();

        if (roleRepository.findByTenantIdAndName(tenantId, request.name()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + request.name());
        }

        Role role = new Role();
        role.setTenant(tenant);
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
            role.setPermissions(perms);
        }
        role = roleRepository.save(role);
        return toDto(role);
    }

    /**
     * Aggiorna ruolo. Se permissionIds presente, sostituisce l'intero set di Permission.
     * Hibernate gestisce la tabella role_permission: rimuove le vecchie associazioni e
     * inserisce le nuove. Replace, non merge.
     */
    @Transactional
    public RoleDto update(UUID id, RoleDto.Update request) {
        return roleRepository.findByIdWithPermissions(id)
                .map(role -> {
                    if (request.name() != null) role.setName(request.name());
                    if (request.description() != null) role.setDescription(request.description());
                    if (request.permissionIds() != null) {
                        role.setPermissions(new HashSet<>(permissionRepository.findAllById(request.permissionIds())));
                    }
                    return roleRepository.save(role);
                })
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public void delete(UUID id) {
        roleRepository.deleteById(id);
    }

    private RoleDto toDto(Role r) {
        Set<UUID> permIds = r.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        return new RoleDto(r.getId(), r.getName(), r.getDescription(), permIds);
    }
}
