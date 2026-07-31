/*
 * WebhookService — Registrazione e gestione webhook per tenant.
 *
 * Alla creazione genera un secret HMAC mostrato una sola volta. Valida che gli eventi
 * siano tra quelli supportati. Espone anche la lista delle delivery (incluso dead letter).
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.WebhookDto;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.Webhook;
import io.secureflow.core.entity.WebhookDelivery;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.WebhookDeliveryRepository;
import io.secureflow.core.repository.WebhookRepository;
import io.secureflow.core.security.TenantContext;
import io.secureflow.core.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final TenantRepository tenantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<WebhookDto> list() {
        TenantContext.getTenantId().orElseThrow();
        return webhookRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookDto get(UUID id) {
        return webhookRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public WebhookDto.Created create(WebhookDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        validateEvents(request.events());
        validateUrl(request.url());

        String secret = generateSecret();
        Webhook webhook = new Webhook();
        webhook.setTenant(tenant);
        webhook.setUrl(request.url());
        webhook.setSecret(secret);
        webhook.setEvents(new HashSet<>(request.events()));
        webhook.setEnabled(true);
        webhook = webhookRepository.save(webhook);

        return new WebhookDto.Created(
                webhook.getId(),
                webhook.getUrl(),
                secret,
                webhook.getEvents(),
                webhook.isEnabled(),
                webhook.getCreatedAt()
        );
    }

    @Transactional
    public WebhookDto update(UUID id, WebhookDto.Update request) {
        return webhookRepository.findById(id)
                .map(webhook -> {
                    if (request.url() != null) {
                        validateUrl(request.url());
                        webhook.setUrl(request.url());
                    }
                    if (request.events() != null) {
                        validateEvents(request.events());
                        webhook.setEvents(new HashSet<>(request.events()));
                    }
                    if (request.enabled() != null) {
                        webhook.setEnabled(request.enabled());
                    }
                    return toDto(webhookRepository.save(webhook));
                })
                .orElse(null);
    }

    @Transactional
    public void delete(UUID id) {
        webhookRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<WebhookDto.DeliveryDto> listDeliveries() {
        TenantContext.getTenantId().orElseThrow();
        return deliveryRepository.findAll().stream()
                .map(this::toDeliveryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WebhookDto.DeliveryDto> listDeadLetters() {
        TenantContext.getTenantId().orElseThrow();
        return deliveryRepository.findByStatus(WebhookDelivery.Status.DEAD_LETTER).stream()
                .map(this::toDeliveryDto)
                .toList();
    }

    private void validateEvents(Set<String> events) {
        for (String event : events) {
            if (!WebhookEventType.ALL.contains(event)) {
                throw new IllegalArgumentException("Unsupported webhook event: " + event);
            }
        }
    }

    private void validateUrl(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalArgumentException("Webhook URL must start with http:// or https://");
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private WebhookDto toDto(Webhook w) {
        return new WebhookDto(w.getId(), w.getUrl(), w.getEvents(), w.isEnabled(), w.getCreatedAt());
    }

    private WebhookDto.DeliveryDto toDeliveryDto(WebhookDelivery d) {
        return new WebhookDto.DeliveryDto(
                d.getId(),
                d.getWebhook().getId(),
                d.getEventType(),
                d.getStatus().name(),
                d.getAttemptCount(),
                d.getLastError(),
                d.getCreatedAt(),
                d.getCompletedAt()
        );
    }
}
