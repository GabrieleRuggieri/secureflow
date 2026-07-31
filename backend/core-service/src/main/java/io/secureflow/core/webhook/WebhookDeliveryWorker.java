/*
 * WebhookDeliveryWorker — Processa la coda di delivery con retry esponenziale.
 *
 * Polling scheduled: prende delivery PENDING con next_attempt_at <= now, POST HTTP
 * firmato HMAC-SHA256. Success → SUCCESS. Failure → backoff 2^attempt secondi, oppure
 * DEAD_LETTER se attempt_count >= max_attempts.
 */
package io.secureflow.core.webhook;

import io.secureflow.core.entity.Webhook;
import io.secureflow.core.entity.WebhookDelivery;
import io.secureflow.core.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "secureflow.webhook.worker-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDeliveryWorker {

    private static final String SIGNATURE_HEADER = "X-SecureFlow-Signature";
    private static final String EVENT_HEADER = "X-SecureFlow-Event";

    private final WebhookDeliveryRepository deliveryRepository;
    private final RestClient.Builder restClientBuilder;

    @Scheduled(fixedDelayString = "${secureflow.webhook.poll-interval-ms:2000}")
    @Transactional
    public void processPending() {
        List<WebhookDelivery> due = deliveryRepository.findDueDeliveries(Instant.now());
        for (WebhookDelivery delivery : due) {
            attemptDelivery(delivery);
        }
    }

    void attemptDelivery(WebhookDelivery delivery) {
        Webhook webhook = delivery.getWebhook();
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        try {
            RestClient client = restClientBuilder.build();
            String signature = sign(webhook.getSecret(), delivery.getPayload());

            client.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, "sha256=" + signature)
                    .header(EVENT_HEADER, delivery.getEventType())
                    .body(delivery.getPayload())
                    .retrieve()
                    .toBodilessEntity();

            delivery.setStatus(WebhookDelivery.Status.SUCCESS);
            delivery.setCompletedAt(Instant.now());
            delivery.setLastError(null);
            deliveryRepository.save(delivery);
            log.debug("Webhook delivery {} succeeded", delivery.getId());
        } catch (Exception e) {
            handleFailure(delivery, e);
        }
    }

    private void handleFailure(WebhookDelivery delivery, Exception e) {
        String error = truncate(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), 1000);
        delivery.setLastError(error);

        if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
            delivery.setStatus(WebhookDelivery.Status.DEAD_LETTER);
            delivery.setCompletedAt(Instant.now());
            log.warn("Webhook delivery {} moved to dead letter after {} attempts: {}",
                    delivery.getId(), delivery.getAttemptCount(), error);
        } else {
            long delaySeconds = 1L << Math.min(delivery.getAttemptCount(), 8);
            delivery.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            delivery.setStatus(WebhookDelivery.Status.PENDING);
            log.debug("Webhook delivery {} failed (attempt {}), retry in {}s: {}",
                    delivery.getId(), delivery.getAttemptCount(), delaySeconds, error);
        }
        deliveryRepository.save(delivery);
    }

    public static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
