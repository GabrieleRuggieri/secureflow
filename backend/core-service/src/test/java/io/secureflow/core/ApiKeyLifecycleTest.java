/*
 * ApiKeyLifecycleTest — Ciclo di vita API key e webhook su H2 (senza Docker).
 *
 * Copre generate/revoke/rotate, hash SHA-256, enqueue delivery e dead letter
 * verso un HttpServer locale. Eseguito da mvn test (no tag integration).
 */
package io.secureflow.core;

import com.sun.net.httpserver.HttpServer;
import io.secureflow.core.domain.ApiKeyService;
import io.secureflow.core.domain.WebhookService;
import io.secureflow.core.dto.ApiKeyDto;
import io.secureflow.core.dto.WebhookDto;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.WebhookDelivery;
import io.secureflow.core.repository.ApiKeyRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.WebhookDeliveryRepository;
import io.secureflow.core.repository.WebhookRepository;
import io.secureflow.core.security.TenantContext;
import io.secureflow.core.webhook.WebhookDeliveryWorker;
import io.secureflow.core.webhook.WebhookEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(ApiKeyLifecycleTest.TestConfig.class)
class ApiKeyLifecycleTest {

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    ApiKeyRepository apiKeyRepository;
    @Autowired
    WebhookRepository webhookRepository;
    @Autowired
    WebhookDeliveryRepository deliveryRepository;
    @Autowired
    ApiKeyService apiKeyService;
    @Autowired
    WebhookService webhookService;
    @Autowired
    WebhookDeliveryWorker webhookDeliveryWorker;

    private Tenant tenant;
    private HttpServer httpServer;
    private final List<ReceivedWebhook> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger httpStatus = new AtomicInteger(200);

    @BeforeEach
    void setUp() throws IOException {
        TenantContext.clear();
        deliveryRepository.deleteAll();
        apiKeyRepository.deleteAll();
        webhookRepository.deleteAll();
        tenantRepository.deleteAll();
        received.clear();
        httpStatus.set(200);

        tenant = new Tenant();
        tenant.setName("Acme");
        tenant.setSlug("acme");
        tenant.setRateLimitPerMinute(100);
        tenant = tenantRepository.save(tenant);

        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/hook", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String signature = exchange.getRequestHeaders().getFirst("X-SecureFlow-Signature");
            String event = exchange.getRequestHeaders().getFirst("X-SecureFlow-Event");
            received.add(new ReceivedWebhook(event, new String(body, StandardCharsets.UTF_8), signature));
            exchange.sendResponseHeaders(httpStatus.get(), 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[0]);
            }
        });
        httpServer.start();

        TenantContext.setTenantId(tenant.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void create_storesHashOnly_andReturnsRawKeyOnce() {
        ApiKeyDto.Created created = apiKeyService.create(new ApiKeyDto.Create("prod", Instant.now().plusSeconds(3600)));

        assertThat(created.rawKey()).startsWith("sf_live_");
        assertThat(created.keyPrefix()).hasSize(16);
        assertThat(created.rawKey()).startsWith(created.keyPrefix());

        var stored = apiKeyRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getKeyHash()).isEqualTo(ApiKeyService.sha256Hex(created.rawKey()));
        assertThat(apiKeyService.isValidRawKey(created.rawKey())).isTrue();
    }

    @Test
    void revoke_marksKeyInvalid() {
        ApiKeyDto.Created created = apiKeyService.create(new ApiKeyDto.Create("to-revoke", null));
        ApiKeyDto revoked = apiKeyService.revoke(created.id());

        assertThat(revoked.revokedAt()).isNotNull();
        assertThat(revoked.valid()).isFalse();
        assertThat(apiKeyService.isValidRawKey(created.rawKey())).isFalse();
    }

    @Test
    void rotate_revokesOldAndIssuesNew() {
        ApiKeyDto.Created original = apiKeyService.create(new ApiKeyDto.Create("rotating", null));
        ApiKeyDto.Created rotated = apiKeyService.rotate(original.id());

        assertThat(rotated.id()).isNotEqualTo(original.id());
        assertThat(apiKeyService.isValidRawKey(original.rawKey())).isFalse();
        assertThat(apiKeyService.isValidRawKey(rotated.rawKey())).isTrue();
    }

    @Test
    void webhook_deliversSignedPayload_onApiKeyCreate() {
        String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/hook";
        WebhookDto.Created webhook = webhookService.create(new WebhookDto.Create(
                url, Set.of(WebhookEventType.API_KEY_CREATED)));

        apiKeyService.create(new ApiKeyDto.Create("hooked", null));
        assertThat(deliveryRepository.findDueDeliveries(Instant.now())).hasSize(1);

        webhookDeliveryWorker.processPending();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo(WebhookEventType.API_KEY_CREATED);
        assertThat(received.get(0).signature()).isEqualTo(
                "sha256=" + WebhookDeliveryWorker.sign(webhook.secret(), received.get(0).body()));
        assertThat(deliveryRepository.findByStatus(WebhookDelivery.Status.SUCCESS)).hasSize(1);
    }

    @Test
    void webhook_movesToDeadLetter_afterMaxAttempts() {
        httpStatus.set(500);
        String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/hook";
        webhookService.create(new WebhookDto.Create(url, Set.of(WebhookEventType.API_KEY_REVOKED)));

        ApiKeyDto.Created created = apiKeyService.create(new ApiKeyDto.Create("dlq", null));
        apiKeyService.revoke(created.id());

        WebhookDelivery delivery = deliveryRepository.findDueDeliveries(Instant.now()).get(0);
        delivery.setMaxAttempts(2);
        delivery.setNextAttemptAt(Instant.now().minusSeconds(1));
        deliveryRepository.save(delivery);

        webhookDeliveryWorker.processPending();
        delivery = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(WebhookDelivery.Status.PENDING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);

        delivery.setNextAttemptAt(Instant.now().minusSeconds(1));
        deliveryRepository.save(delivery);
        webhookDeliveryWorker.processPending();

        delivery = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(WebhookDelivery.Status.DEAD_LETTER);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(received).hasSize(2);
    }

    private record ReceivedWebhook(String event, String body, String signature) {}

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue("test")
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("tenantId", "acme")
                    .build();
        }

        @Bean
        WebhookDeliveryWorker webhookDeliveryWorker(
                WebhookDeliveryRepository deliveryRepository,
                RestClient.Builder restClientBuilder) {
            return new WebhookDeliveryWorker(deliveryRepository, restClientBuilder);
        }
    }
}
