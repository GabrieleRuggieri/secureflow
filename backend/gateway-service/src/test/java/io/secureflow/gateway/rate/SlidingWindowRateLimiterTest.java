/*
 * SlidingWindowRateLimiterTest — Verifica rate limiter sotto carico concorrente.
 *
 * Usa Redis embedded via testcontainers-redis se disponibile; altrimenti skip.
 * Tag integration per esclusione da mvn test default.
 */
package io.secureflow.gateway.rate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "secureflow.core-service-url=http://localhost:8081",
        "secureflow.upstream-base-url=http://localhost:8081"
})
@Testcontainers
@Tag("integration")
class SlidingWindowRateLimiterTest {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8-alpine"));

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    SlidingWindowRateLimiter rateLimiter;

    @Test
    void concurrentRequests_respectLimit() {
        UUID tenantId = UUID.randomUUID();
        int limit = 10;

        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();

        Flux.range(0, 30)
                .flatMap(i -> rateLimiter.tryAcquire(tenantId, limit)
                        .doOnNext(r -> {
                            if (r.allowed()) allowed.incrementAndGet();
                            else denied.incrementAndGet();
                        }), 30)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(allowed.get()).isEqualTo(limit);
        assertThat(denied.get()).isEqualTo(20);
    }

    @Test
    void underLimit_allAllowed() {
        UUID tenantId = UUID.randomUUID();
        Mono<Long> count = Flux.range(0, 5)
                .concatMap(i -> rateLimiter.tryAcquire(tenantId, 10))
                .filter(SlidingWindowRateLimiter.RateLimitResult::allowed)
                .count();

        StepVerifier.create(count)
                .expectNext(5L)
                .verifyComplete();
    }
}
