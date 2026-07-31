/*
 * SlidingWindowRateLimiter — Rate limit distribuito su Redis con script Lua atomico.
 *
 * ZSET ordinato per timestamp: rimuove entry fuori finestra, conta, e se sotto soglia
 * inserisce la request corrente. Una sola round-trip Redis evita race condition.
 */
package io.secureflow.gateway.rate;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimiter {

    private final ReactiveStringRedisTemplate redis;

    @Value("${secureflow.rate-limit-window-ms:60000}")
    private long windowMs;

    private final RedisScript<List> script = RedisScript.of(
            new ClassPathResource("rate_limit.lua"),
            List.class
    );

    public Mono<RateLimitResult> tryAcquire(UUID tenantId, int limitPerMinute) {
        String key = "ratelimit:" + tenantId;
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID();

        return redis.execute(
                        script,
                        Collections.singletonList(key),
                        String.valueOf(windowMs),
                        String.valueOf(limitPerMinute),
                        String.valueOf(now),
                        member
                )
                .next()
                .map(result -> {
                    @SuppressWarnings("unchecked")
                    List<Long> values = (List<Long>) result;
                    boolean allowed = values.get(0) == 1L;
                    long remaining = values.get(1);
                    return new RateLimitResult(allowed, remaining, limitPerMinute);
                })
                .defaultIfEmpty(new RateLimitResult(false, 0, limitPerMinute));
    }

    public record RateLimitResult(boolean allowed, long remaining, int limit) {}
}
