/*
 * OpenApiConfig — Documentazione SpringDoc per il Gateway (WebFlux).
 */
package io.secureflow.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SecureFlow Gateway API")
                        .description("Reactive gateway: API key auth, rate limiting, proxy, audit publish")
                        .version("0.1.0"));
    }
}
