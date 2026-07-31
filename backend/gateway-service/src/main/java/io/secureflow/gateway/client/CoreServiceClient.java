/*
 * CoreServiceClient — Validazione API key verso Core Service con cache Redis e circuit breaker.
 *
 * Cache hit: evita round-trip. Cache miss: chiamata HTTP con Resilience4j CircuitBreaker.
 * Token interno X-Internal-Token per autenticazione M2M.
 */
package io.secureflow.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

@Component
@Slf4j
public class CoreServiceClient {

    private static final String CACHE_PREFIX = "apikey:";

    private final WebClient webClient;
    private final ReactiveStringRedisTemplate redis;
    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;
    private final String internalToken;
    private final Duration cacheTtl;

    public CoreServiceClient(
            WebClient.Builder webClientBuilder,
            ReactiveStringRedisTemplate redis,
            CircuitBreaker coreServiceCircuitBreaker,
            ObjectMapper objectMapper,
            @Value("${secureflow.core-service-url}") String coreServiceUrl,
            @Value("${secureflow.internal-token}") String internalToken,
            @Value("${secureflow.api-key-cache-ttl-seconds:60}") long cacheTtlSeconds) {
        this.webClient = webClientBuilder.baseUrl(coreServiceUrl).build();
        this.redis = redis;
        this.circuitBreaker = coreServiceCircuitBreaker;
        this.objectMapper = objectMapper;
        this.internalToken = internalToken;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public Mono<ValidatedApiKey> validate(String rawApiKey) {
        String cacheKey = CACHE_PREFIX + sha256Hex(rawApiKey);
        return redis.opsForValue().get(cacheKey)
                .flatMap(this::deserialize)
                .switchIfEmpty(Mono.defer(() -> fetchFromCore(rawApiKey)
                        .flatMap(result -> cache(cacheKey, result).thenReturn(result))));
    }

    private Mono<ValidatedApiKey> fetchFromCore(String rawApiKey) {
        return webClient.post()
                .uri("/internal/api-keys/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", internalToken)
                .bodyValue(Map.of("apiKey", rawApiKey))
                .retrieve()
                .bodyToMono(ValidatedApiKey.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(ex -> {
                    log.warn("Core Service validation failed: {}", ex.toString());
                    return Mono.just(ValidatedApiKey.invalid());
                });
    }

    private Mono<Boolean> cache(String cacheKey, ValidatedApiKey result) {
        if (!result.valid()) {
            return Mono.just(false);
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return redis.opsForValue().set(cacheKey, json, cacheTtl);
        } catch (Exception e) {
            return Mono.just(false);
        }
    }

    private Mono<ValidatedApiKey> deserialize(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, ValidatedApiKey.class));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
