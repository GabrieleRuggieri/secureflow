/*
 * ApiKeyController — Endpoint REST per il ciclo di vita delle API key.
 *
 * POST crea e restituisce la rawKey una sola volta. POST /{id}/revoke e /{id}/rotate
 * per revoca e rotazione. Permessi: apikey:read|create|revoke|rotate.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.ApiKeyService;
import io.secureflow.core.dto.ApiKeyDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @RequiresPermission("apikey:read")
    public List<ApiKeyDto> list() {
        return apiKeyService.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("apikey:read")
    public ResponseEntity<ApiKeyDto> get(@PathVariable UUID id) {
        ApiKeyDto dto = apiKeyService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("apikey:create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyDto.Created create(@Valid @RequestBody ApiKeyDto.Create request) {
        return apiKeyService.create(request);
    }

    @PostMapping("/{id}/revoke")
    @RequiresPermission("apikey:revoke")
    public ResponseEntity<ApiKeyDto> revoke(@PathVariable UUID id) {
        ApiKeyDto dto = apiKeyService.revoke(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/rotate")
    @RequiresPermission("apikey:rotate")
    public ResponseEntity<ApiKeyDto.Created> rotate(@PathVariable UUID id) {
        ApiKeyDto.Created dto = apiKeyService.rotate(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
