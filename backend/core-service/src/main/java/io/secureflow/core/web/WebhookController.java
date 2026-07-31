/*
 * WebhookController — Endpoint REST per webhook e delivery.
 *
 * CRUD webhook + lista delivery / dead-letter. Permessi webhook:read|create|update|delete.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.WebhookService;
import io.secureflow.core.dto.WebhookDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @RequiresPermission("webhook:read")
    public List<WebhookDto> list() {
        return webhookService.list();
    }

    @GetMapping("/deliveries")
    @RequiresPermission("webhook:read")
    public List<WebhookDto.DeliveryDto> listDeliveries() {
        return webhookService.listDeliveries();
    }

    @GetMapping("/deliveries/dead-letter")
    @RequiresPermission("webhook:read")
    public List<WebhookDto.DeliveryDto> listDeadLetters() {
        return webhookService.listDeadLetters();
    }

    @GetMapping("/{id}")
    @RequiresPermission("webhook:read")
    public ResponseEntity<WebhookDto> get(@PathVariable UUID id) {
        WebhookDto dto = webhookService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("webhook:create")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookDto.Created create(@Valid @RequestBody WebhookDto.Create request) {
        return webhookService.create(request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("webhook:update")
    public ResponseEntity<WebhookDto> update(@PathVariable UUID id, @Valid @RequestBody WebhookDto.Update request) {
        WebhookDto dto = webhookService.update(id, request);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("webhook:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        webhookService.delete(id);
    }
}
