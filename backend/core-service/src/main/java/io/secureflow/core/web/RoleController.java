/*
 * RoleController — Endpoint REST per la gestione ruoli.
 *
 * /api/roles: CRUD. I ruoli includono permessi (permissionIds). Create/Update
 * accettano un set di UUID permission; il service gestisce la relazione ManyToMany.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.RoleService;
import io.secureflow.core.dto.RoleDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequiresPermission("role:read")
    public List<RoleDto> list() {
        return roleService.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("role:read")
    public ResponseEntity<RoleDto> get(@PathVariable UUID id) {
        RoleDto dto = roleService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("role:create")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto create(@Valid @RequestBody RoleDto.Create request) {
        return roleService.create(request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("role:update")
    public ResponseEntity<RoleDto> update(@PathVariable UUID id, @Valid @RequestBody RoleDto.Update request) {
        RoleDto dto = roleService.update(id, request);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("role:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roleService.delete(id);
    }
}
