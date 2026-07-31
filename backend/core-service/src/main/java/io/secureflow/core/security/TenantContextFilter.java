/*
 * TenantContextFilter — Estrae il tenant dal JWT e imposta TenantContext.
 *
 * Esegue per ogni richiesta autenticata. Legge "tenantId" dal JWT: se è UUID lo usa
 * direttamente; se è slug (es. "default-tenant") lo risolve via TenantRepository.
 * clear() in finally per evitare che il tenant "percoli" in altre richieste.
 */
package io.secureflow.core.security;

import io.secureflow.core.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Extracts tenant ID from the JWT and sets it in TenantContext for the Hibernate filter.
 * Supports both UUID and tenant slug (e.g. "default-tenant").
 * Clears the context at the end of each request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_CLAIM = "tenantId";

    private final TenantRepository tenantRepository;

    public TenantContextFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Per ogni richiesta: se c'è un JWT valido, estrae tenantId e lo mette in TenantContext.
     * resolveTenantId: se è UUID valido lo usa; altrimenti è slug (es. "default-tenant") e
     * lo risolve via TenantRepository.findBySlug. Il finally clear() è fondamentale: il
     * thread pool riusa i thread; senza clear il tenant della richiesta precedente potrebbe
     * "percolare" in quella successiva.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String tenantIdStr = jwt.getClaimAsString(TENANT_ID_CLAIM);
                if (tenantIdStr != null) {
                    resolveTenantId(tenantIdStr).ifPresent(TenantContext::setTenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Se tenantIdStr è un UUID valido, lo restituisce. Altrimenti lo interpreta come slug
     * e fa lookup su Tenant. L'admin Keycloak ha tenantId="default-tenant" (attributo utente);
     * la V3 migration crea quel tenant con UUID fisso.
     */
    private java.util.Optional<UUID> resolveTenantId(String tenantIdStr) {
        try {
            return java.util.Optional.of(UUID.fromString(tenantIdStr));
        } catch (IllegalArgumentException e) {
            return tenantRepository.findBySlug(tenantIdStr).map(t -> t.getId());
        }
    }
}
