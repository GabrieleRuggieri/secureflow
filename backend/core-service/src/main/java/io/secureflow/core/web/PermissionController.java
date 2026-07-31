/*
 * PermissionController — Endpoint REST per la gestione permessi.
 *
 * /api/permissions: CRUD. I permessi sono atomi del RBAC; resource e action sono
 * immutabili dopo la creazione. Solo description può essere aggiornata.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.PermissionService;
import io.secureflow.core.dto.PermissionDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @RequiresPermission("permission:read")
    public List<PermissionDto> list() {
        return permissionService.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("permission:read")
    public ResponseEntity<PermissionDto> get(@PathVariable UUID id) {
        PermissionDto dto = permissionService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("permission:create")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDto create(@Valid @RequestBody PermissionDto.Create request) {
        return permissionService.create(request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("permission:update")
    public ResponseEntity<PermissionDto> update(@PathVariable UUID id, @Valid @RequestBody PermissionDto.Update request) {
        PermissionDto dto = permissionService.update(id, request);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("permission:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        permissionService.delete(id);
    }
}
