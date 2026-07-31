/*
 * PermissionService — Logica di business per la gestione permessi.
 *
 * Validazione: (resource, action) univoco per tenant. I permessi sono atomi del RBAC;
 * vengono assegnati ai Role, non direttamente agli User. Il formato "resource:action"
 * è usato da @RequiresPermission e RbacPermissionEvaluator.
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.PermissionDto;
import io.secureflow.core.entity.Permission;
import io.secureflow.core.repository.PermissionRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<PermissionDto> list() {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        return permissionRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionDto get(UUID id) {
        return permissionRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Crea un permesso. Validazione: (resource, action) univoco per tenant. I permessi
     * sono atomi; non si possono avere due "tenant:create" nello stesso tenant.
     */
    @Transactional
    public PermissionDto create(PermissionDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        var tenant = tenantRepository.findById(tenantId).orElseThrow();

        if (permissionRepository.findByTenantIdAndResourceAndAction(tenantId, request.resource(), request.action()).isPresent()) {
            throw new IllegalArgumentException("Permission already exists: " + request.resource() + ":" + request.action());
        }

        Permission perm = new Permission();
        perm.setTenant(tenant);
        perm.setResource(request.resource());
        perm.setAction(request.action());
        perm.setDescription(request.description());
        perm = permissionRepository.save(perm);
        return toDto(perm);
    }

    @Transactional
    public PermissionDto update(UUID id, PermissionDto.Update request) {
        return permissionRepository.findById(id)
                .map(perm -> {
                    if (request.description() != null) perm.setDescription(request.description());
                    return permissionRepository.save(perm);
                })
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public void delete(UUID id) {
        permissionRepository.deleteById(id);
    }

    private PermissionDto toDto(Permission p) {
        return new PermissionDto(p.getId(), p.getResource(), p.getAction(), p.getDescription());
    }
}
