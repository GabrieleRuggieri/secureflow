/*
 * RateLimitFilter — Applica sliding window Redis per tenant dopo autenticazione API key.
 *
 * 429 + header X-RateLimit-* se superato. Altrimenti propaga remaining negli attributes.
 */
package io.secureflow.gateway.filter;

import io.secureflow.gateway.rate.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements WebFilter, Ordered {

    private final SlidingWindowRateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") || path.equals("/health")
                || path.startsWith("/swagger-ui") || path.startsWith("/api-docs") || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        UUID tenantId = GatewayAttributes.asUuid(exchange.getAttribute(GatewayAttributes.TENANT_ID));
        Integer limit = exchange.getAttribute(GatewayAttributes.RATE_LIMIT);
        if (tenantId == null || limit == null) {
            return chain.filter(exchange);
        }

        return rateLimiter.tryAcquire(tenantId, limit)
                .flatMap(result -> {
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(result.limit()));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
                    if (!result.allowed()) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getAttributes().put(GatewayAttributes.RATE_REMAINING, result.remaining());
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
