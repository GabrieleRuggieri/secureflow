/*
 * AuditServiceTest — Persistenza idempotente e search filtrato (H2, no Kafka).
 */
package io.secureflow.core;

import io.secureflow.core.domain.AuditService;
import io.secureflow.core.dto.AuditEventDto;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.repository.AuditEventRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(AuditServiceTest.TestConfig.class)
class AuditServiceTest {

    @Autowired
    AuditService auditService;
    @Autowired
    AuditEventRepository auditEventRepository;
    @Autowired
    TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditEventRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = new Tenant();
        tenant.setName("Audit Co");
        tenant.setSlug("audit-co");
        tenant.setRateLimitPerMinute(60);
        tenant = tenantRepository.save(tenant);
        TenantContext.setTenantId(tenant.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void persist_isIdempotentOnSameEventId() {
        AuditEventDto.Message msg = new AuditEventDto.Message(
                "evt-1",
                tenant.getId(),
                "sf_live_",
                "GET",
                "/api/demo",
                200,
                12L,
                "success",
                Instant.parse("2026-07-31T10:00:00Z")
        );

        AuditEventDto first = auditService.persist(msg);
        AuditEventDto second = auditService.persist(msg);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void search_filtersByOutcomeAndTimeRange() {
        Instant t1 = Instant.parse("2026-07-31T10:00:00Z");
        Instant t2 = Instant.parse("2026-07-31T11:00:00Z");
        Instant t3 = Instant.parse("2026-07-31T12:00:00Z");

        auditService.persist(msg("a", "success", t1));
        auditService.persist(msg("b", "denied", t2));
        auditService.persist(msg("c", "success", t3));

        var success = auditService.search("success", null, null, PageRequest.of(0, 10));
        assertThat(success.getContent()).extracting(AuditEventDto::eventId)
                .containsExactly("c", "a");

        var window = auditService.search(null, t2, t3, PageRequest.of(0, 10));
        assertThat(window.getContent()).extracting(AuditEventDto::eventId)
                .containsExactly("c", "b");
    }

    private AuditEventDto.Message msg(String id, String outcome, Instant at) {
        return new AuditEventDto.Message(
                id, tenant.getId(), "sf_live_", "GET", "/x", 200, 1L, outcome, at);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .claim("tenantId", UUID.randomUUID().toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}
