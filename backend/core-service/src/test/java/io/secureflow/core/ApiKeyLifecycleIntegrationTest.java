/*
 * ApiKeyLifecycleIntegrationTest — Ciclo di vita API key + delivery webhook.
 *
 * Generazione (hash SHA-256, prefix, rawKey one-time), revoca, rotazione.
 * Webhook: enqueue su create/revoke e delivery HTTP con HMAC verso un server locale.
 * Tag "integration": richiede Docker (Testcontainers MySQL).
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
import org.junit.jupiter.api.Tag;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Import(ApiKeyLifecycleIntegrationTest.TestConfig.class)
class ApiKeyLifecycleIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("secureflow")
            .withUsername("secureflow")
            .withPassword("secureflow")
            .withReuse(true);

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("secureflow.webhook.worker-enabled", () -> "false");
    }

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
            int status = httpStatus.get();
            exchange.sendResponseHeaders(status, 0);
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
        assertThat(stored.getKeyHash()).doesNotContain(created.rawKey());
        assertThat(apiKeyService.isValidRawKey(created.rawKey())).isTrue();
    }

    @Test
    void revoke_marksKeyInvalid() {
        ApiKeyDto.Created created = apiKeyService.create(new ApiKeyDto.Create("to-revoke", null));
        assertThat(apiKeyService.isValidRawKey(created.rawKey())).isTrue();

        ApiKeyDto revoked = apiKeyService.revoke(created.id());
        assertThat(revoked).isNotNull();
        assertThat(revoked.revokedAt()).isNotNull();
        assertThat(revoked.valid()).isFalse();
        assertThat(apiKeyService.isValidRawKey(created.rawKey())).isFalse();
    }

    @Test
    void rotate_revokesOldAndIssuesNew() {
        ApiKeyDto.Created original = apiKeyService.create(new ApiKeyDto.Create("rotating", null));
        ApiKeyDto.Created rotated = apiKeyService.rotate(original.id());

        assertThat(rotated).isNotNull();
        assertThat(rotated.id()).isNotEqualTo(original.id());
        assertThat(rotated.rawKey()).isNotEqualTo(original.rawKey());
        assertThat(apiKeyService.isValidRawKey(original.rawKey())).isFalse();
        assertThat(apiKeyService.isValidRawKey(rotated.rawKey())).isTrue();
    }

    @Test
    void webhook_deliversSignedPayload_onApiKeyCreate() {
        String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/hook";
        WebhookDto.Created webhook = webhookService.create(new WebhookDto.Create(
                url,
                Set.of(WebhookEventType.API_KEY_CREATED)
        ));

        apiKeyService.create(new ApiKeyDto.Create("hooked", null));

        List<WebhookDelivery> pending = deliveryRepository.findDueDeliveries(Instant.now());
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo(WebhookEventType.API_KEY_CREATED);

        webhookDeliveryWorker.processPending();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo(WebhookEventType.API_KEY_CREATED);
        assertThat(received.get(0).signature()).isEqualTo(
                "sha256=" + WebhookDeliveryWorker.sign(webhook.secret(), received.get(0).body())
        );
        assertThat(deliveryRepository.findDueDeliveries(Instant.now())).isEmpty();
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

        /** Worker disabilitato via property; bean esplicito per i test che lo invocano. */
        @Bean
        WebhookDeliveryWorker webhookDeliveryWorker(
                WebhookDeliveryRepository deliveryRepository,
                RestClient.Builder restClientBuilder) {
            return new WebhookDeliveryWorker(deliveryRepository, restClientBuilder);
        }
    }
}
