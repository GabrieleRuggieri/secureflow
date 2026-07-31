/*
 * WebhookDispatcher — Accoda delivery per i webhook sottoscritti a un evento.
 *
 * Chiamato dai domain service dopo create/revoke/rotate. Crea WebhookDelivery PENDING
 * per ogni webhook enabled del tenant che include l'eventType. Il worker processa la coda.
 */
package io.secureflow.core.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.Webhook;
import io.secureflow.core.entity.WebhookDelivery;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.WebhookDeliveryRepository;
import io.secureflow.core.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcher {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void dispatch(UUID tenantId, String eventType, Map<String, Object> data) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("Webhook dispatch skipped: tenant {} not found", tenantId);
            return;
        }

        List<Webhook> webhooks = webhookRepository.findByTenant_IdAndEnabledTrue(tenantId).stream()
                .filter(w -> w.subscribesTo(eventType))
                .toList();

        if (webhooks.isEmpty()) {
            return;
        }

        String payload = buildPayload(tenantId, eventType, data);
        Instant now = Instant.now();

        for (Webhook webhook : webhooks) {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setTenant(tenant);
            delivery.setWebhook(webhook);
            delivery.setEventType(eventType);
            delivery.setPayload(payload);
            delivery.setStatus(WebhookDelivery.Status.PENDING);
            delivery.setNextAttemptAt(now);
            deliveryRepository.save(delivery);
        }

        log.debug("Enqueued {} webhook deliveries for event {} tenant {}", webhooks.size(), eventType, tenantId);
    }

    private String buildPayload(UUID tenantId, String eventType, Map<String, Object> data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", UUID.randomUUID().toString());
        envelope.put("type", eventType);
        envelope.put("tenantId", tenantId.toString());
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("data", data);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize webhook payload", e);
        }
    }
}
