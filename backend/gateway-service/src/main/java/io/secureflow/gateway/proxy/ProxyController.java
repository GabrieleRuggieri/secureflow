/*
 * ProxyController — Forwarding reattivo verso upstream configurato.
 *
 * Path /proxy/** viene inoltrato a upstream-base-url + path relativo, preservando
 * method, query e body. Header hop-by-hop esclusi. Tenant propagato in X-Tenant-Id.
 */
package io.secureflow.gateway.proxy;

import io.secureflow.gateway.filter.GatewayAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
public class ProxyController {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final WebClient webClient;
    private final String upstreamBaseUrl;

    public ProxyController(
            WebClient.Builder webClientBuilder,
            @Value("${secureflow.upstream-base-url}") String upstreamBaseUrl) {
        this.webClient = webClientBuilder.build();
        this.upstreamBaseUrl = upstreamBaseUrl.endsWith("/")
                ? upstreamBaseUrl.substring(0, upstreamBaseUrl.length() - 1)
                : upstreamBaseUrl;
    }

    @RequestMapping("/proxy/**")
    public Mono<Void> proxy(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        String relative = path.startsWith("/proxy") ? path.substring("/proxy".length()) : path;
        if (relative.isEmpty()) {
            relative = "/";
        }

        String query = request.getURI().getRawQuery();
        String target = upstreamBaseUrl + relative + (query != null ? "?" + query : "");

        HttpMethod method = request.getMethod();
        UUID tenantId = GatewayAttributes.asUuid(exchange.getAttribute(GatewayAttributes.TENANT_ID));

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(URI.create(target))
                .headers(headers -> copyHeaders(request.getHeaders(), headers, tenantId));

        Mono<Void> call;
        if (method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.DELETE) {
            call = spec.retrieve()
                    .toEntityFlux(DataBuffer.class)
                    .flatMap(response -> writeResponse(exchange, response.getStatusCode().value(),
                            response.getHeaders(), response.getBody()));
        } else {
            call = spec.body(BodyInserters.fromDataBuffers(request.getBody()))
                    .retrieve()
                    .toEntityFlux(DataBuffer.class)
                    .flatMap(response -> writeResponse(exchange, response.getStatusCode().value(),
                            response.getHeaders(), response.getBody()));
        }
        return call;
    }

    private void copyHeaders(HttpHeaders source, HttpHeaders target, UUID tenantId) {
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP.contains(name.toLowerCase()) && !"x-api-key".equalsIgnoreCase(name)) {
                target.addAll(name, values);
            }
        });
        if (tenantId != null) {
            target.set("X-Tenant-Id", tenantId.toString());
        }
    }

    private Mono<Void> writeResponse(
            ServerWebExchange exchange,
            int status,
            HttpHeaders headers,
            reactor.core.publisher.Flux<DataBuffer> body) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatusCode.valueOf(status));
        headers.forEach((name, values) -> {
            if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                exchange.getResponse().getHeaders().put(name, values);
            }
        });
        return exchange.getResponse().writeWith(body);
    }
}
