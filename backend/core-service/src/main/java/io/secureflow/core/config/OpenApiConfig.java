/*
 * OpenApiConfig — Documentazione SpringDoc per il Core Service.
 */
package io.secureflow.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI secureFlowOpenApi() {
        final String bearer = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("SecureFlow Core API")
                        .description("Multi-tenant RBAC, API keys, webhooks e audit log")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components().addSecuritySchemes(bearer,
                        new SecurityScheme()
                                .name(bearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
