/*
 * AuditWebFilter — Pubblica AuditEvent a fine richiesta (dopo proxy), fire-and-forget.
 *
 * Usa doFinally sulla catena per catturare status e durata anche in caso di errore.
 */
package io.secureflow.gateway.filter;

import io.secureflow.gateway.audit.AuditEvent;
import io.secureflow.gateway.audit.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditWebFilter implements WebFilter, Ordered {

    private final AuditEventPublisher auditEventPublisher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") || path.equals("/health")
                || path.startsWith("/swagger-ui") || path.startsWith("/api-docs") || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange)
                .doFinally(signal -> publishAudit(exchange));
    }

    private void publishAudit(ServerWebExchange exchange) {
        Long start = exchange.getAttribute(GatewayAttributes.START_NANOS);
        long durationMs = start != null ? (System.nanoTime() - start) / 1_000_000L : 0L;
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status != null ? status.value() : 500;
        String outcome = statusCode >= 500 ? "error"
                : statusCode == 429 ? "rate_limited"
                : statusCode >= 400 ? "denied"
                : "success";

        UUID tenantId = GatewayAttributes.asUuid(exchange.getAttribute(GatewayAttributes.TENANT_ID));
        String keyPrefix = exchange.getAttribute(GatewayAttributes.KEY_PREFIX);

        // Auth failures never set tenantId (invalid/missing key). Still audit as denied
        // on the bootstrap default tenant so the dashboard shows 401 attempts.
        if (tenantId == null) {
            if (statusCode != 401 && statusCode != 403) {
                return;
            }
            tenantId = DEFAULT_AUDIT_TENANT;
            if (keyPrefix == null || keyPrefix.isBlank()) {
                keyPrefix = "unknown";
            }
        }

        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                keyPrefix,
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().value(),
                statusCode,
                durationMs,
                outcome
        ));
    }

    /** Same UUID as Flyway V3 default-tenant — used only for unauthenticated denials. */
    private static final UUID DEFAULT_AUDIT_TENANT =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
