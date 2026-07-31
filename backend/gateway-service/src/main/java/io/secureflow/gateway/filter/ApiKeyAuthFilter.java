/*
 * ApiKeyAuthFilter — Estrae X-API-Key, valida via CoreServiceClient, popola exchange attributes.
 *
 * Ordine alto: esegue prima del rate limit e del proxy. 401 se chiave assente/invalida.
 */
package io.secureflow.gateway.filter;

import io.secureflow.gateway.client.CoreServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements WebFilter, Ordered {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final CoreServiceClient coreServiceClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        exchange.getAttributes().put(GatewayAttributes.START_NANOS, System.nanoTime());

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            exchange.getAttributes().put(GatewayAttributes.KEY_PREFIX, "missing");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        exchange.getAttributes().put(GatewayAttributes.KEY_PREFIX, abbreviatePrefix(apiKey.trim()));

        return coreServiceClient.validate(apiKey.trim())
                .flatMap(result -> {
                    if (!result.valid()) {
                        if (result.keyPrefix() != null) {
                            exchange.getAttributes().put(GatewayAttributes.KEY_PREFIX, result.keyPrefix());
                        }
                        if (result.tenantId() != null) {
                            exchange.getAttributes().put(GatewayAttributes.TENANT_ID, result.tenantId());
                        }
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getAttributes().put(GatewayAttributes.API_KEY, apiKey.trim());
                    exchange.getAttributes().put(GatewayAttributes.TENANT_ID, result.tenantId());
                    exchange.getAttributes().put(GatewayAttributes.KEY_PREFIX, result.keyPrefix());
                    exchange.getAttributes().put(GatewayAttributes.KEY_ID, result.keyId());
                    exchange.getAttributes().put(GatewayAttributes.RATE_LIMIT, result.rateLimitPerMinute());
                    return chain.filter(exchange);
                });
    }

    private static String abbreviatePrefix(String rawKey) {
        return rawKey.length() <= 20 ? rawKey : rawKey.substring(0, 20);
    }

    private boolean isPublic(String path) {
        return path.startsWith("/actuator")
                || path.equals("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
