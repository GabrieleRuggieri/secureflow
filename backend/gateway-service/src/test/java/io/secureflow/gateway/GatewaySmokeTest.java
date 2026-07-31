package io.secureflow.gateway;

import io.secureflow.gateway.client.CoreServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySmokeTest {

    @Test
    void rateLimitScriptIsOnClasspath() {
        assertThat(new ClassPathResource("rate_limit.lua").exists()).isTrue();
    }

    @Test
    void apiKeyHashIsStable() {
        String hash = CoreServiceClient.sha256Hex("sf_live_test");
        assertThat(hash).hasSize(64);
        assertThat(CoreServiceClient.sha256Hex("sf_live_test")).isEqualTo(hash);
        assertThat(CoreServiceClient.sha256Hex("other")).isNotEqualTo(hash);
    }
}
