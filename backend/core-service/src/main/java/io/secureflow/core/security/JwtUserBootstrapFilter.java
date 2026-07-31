/*
 * JwtUserBootstrapFilter — Dopo TenantContext, provvede User RBAC dal JWT se assente.
 */
package io.secureflow.core.security;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtUserBootstrapFilter extends OncePerRequestFilter {

    private final JwtUserBootstrapService bootstrapService;

    public JwtUserBootstrapFilter(JwtUserBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            UUID tenantId = TenantContext.getTenantId().orElse(null);
            if (tenantId != null) {
                bootstrapService.ensureUser(jwt, tenantId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
