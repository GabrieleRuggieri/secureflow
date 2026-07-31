/*
 * AuditPipelineIntegrationTest — Publish Kafka → consumer → persist → query.
 *
 * Tag "integration": richiede Docker (Testcontainers MySQL + Kafka).
 */
package io.secureflow.core;

import io.secureflow.core.domain.AuditService;
import io.secureflow.core.dto.AuditEventDto;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.repository.AuditEventRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.security.TenantContext;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "secureflow.audit.consumer-enabled=true",
        "secureflow.audit.topic=audit-events",
        "spring.kafka.listener.auto-startup=true"
})
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Import(AuditPipelineIntegrationTest.TestConfig.class)
class AuditPipelineIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("secureflow")
            .withUsername("secureflow")
            .withPassword("secureflow")
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("secureflow.webhook.worker-enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    AuditEventRepository auditEventRepository;
    @Autowired
    AuditService auditService;

    private Tenant tenant;
    private KafkaTemplate<String, AuditEventDto.Message> producer;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditEventRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = new Tenant();
        tenant.setName("Pipeline Co");
        tenant.setSlug("pipeline-co");
        tenant.setRateLimitPerMinute(100);
        tenant = tenantRepository.save(tenant);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void kafkaMessage_isPersistedAndQueryable() throws Exception {
        String eventId = UUID.randomUUID().toString();
        AuditEventDto.Message message = new AuditEventDto.Message(
                eventId,
                tenant.getId(),
                "sf_live_abc",
                "GET",
                "/proxy/demo",
                200,
                42L,
                "success",
                Instant.parse("2026-07-31T14:00:00Z")
        );

        producer.send("audit-events", tenant.getId().toString(), message).get();

        boolean persisted = false;
        for (int i = 0; i < 60; i++) {
            if (auditEventRepository.existsByEventId(eventId)) {
                persisted = true;
                break;
            }
            Thread.sleep(500);
        }
        assertThat(persisted).as("audit event should be consumed within 30s").isTrue();

        TenantContext.setTenantId(tenant.getId());
        var page = auditService.search("success", null, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(AuditEventDto::eventId).contains(eventId);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .claim("tenantId", "pipeline-co")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}
