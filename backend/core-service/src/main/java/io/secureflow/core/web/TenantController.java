/*
 * TenantController — Endpoint REST per la gestione tenant.
 *
 * /api/tenants: CRUD completo. Ogni metodo richiede il permesso corrispondente
 * (tenant:read, tenant:create, ecc.) verificato da @RequiresPermission. I controller
 * sono thin: delegano ai domain service e mappano solo HTTP↔DTO.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.TenantService;
import io.secureflow.core.dto.TenantDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @RequiresPermission("tenant:read")
    public List<TenantDto> list() {
        return tenantService.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("tenant:read")
    public ResponseEntity<TenantDto> get(@PathVariable UUID id) {
        TenantDto dto = tenantService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("tenant:create")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDto create(@Valid @RequestBody TenantDto.Create request) {
        return tenantService.create(request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("tenant:update")
    public ResponseEntity<TenantDto> update(@PathVariable UUID id, @Valid @RequestBody TenantDto.Update request) {
        TenantDto dto = tenantService.update(id, request);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("tenant:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tenantService.delete(id);
    }
}
