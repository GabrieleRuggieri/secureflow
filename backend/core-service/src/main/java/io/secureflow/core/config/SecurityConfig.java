/*
 * SecurityConfig — Configurazione Spring Security per il Core Service.
 *
 * Tre filter chain: /api/** (JWT + tenant filter), /actuator/** (permitAll per health),
 * default (JWT per altre route). OAuth2 Resource Server con JWT da Keycloak. TenantContextFilter
 * prima del BearerToken per avere il tenant dopo la validazione del token. Method security
 * con PermissionEvaluator custom per RBAC.
 */
package io.secureflow.core.config;

import io.secureflow.core.security.RbacPermissionEvaluator;
import io.secureflow.core.security.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final TenantContextFilter tenantContextFilter;
    private final RbacPermissionEvaluator permissionEvaluator;

    public SecurityConfig(TenantContextFilter tenantContextFilter,
                         RbacPermissionEvaluator permissionEvaluator) {
        this.tenantContextFilter = tenantContextFilter;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Configura il PermissionEvaluator per @PreAuthorize("hasPermission(...)"). Spring
     * Method Security usa questo handler per risolvere hasPermission; senza, non saprebbe
     * a chi delegare. Usiamo principalmente @RequiresPermission (aspect), ma il bean
     * serve per eventuale uso diretto di hasPermission.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /**
     * Chain 1: /api/** — richiede JWT valido. addFilterBefore(TenantContextFilter): esegue
     * prima del BearerTokenAuthenticationFilter così dopo la validazione JWT abbiamo il
     * SecurityContext popolato e il TenantContextFilter può leggere il token. Session
     * STATELESS: no cookie, ogni request deve avere Authorization: Bearer.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Chain 2: /actuator/** — permitAll per health check, readiness, liveness. I probe
     * Kubernetes non hanno JWT; devono poter raggiungere /actuator/health senza 401.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Chain 3: default — cattura tutto il resto. Stessa config di /api ma per path non
     * matched. Order 3: Spring valuta le chain in ordine; la prima che matcha vince.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
