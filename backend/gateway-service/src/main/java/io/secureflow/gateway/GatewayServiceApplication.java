/*
 * GatewayServiceApplication — Entry point del Gateway reattivo.
 *
 * Pipeline: estrazione API key → validazione (Redis cache / Core Service) →
 * rate limit sliding window → proxy upstream → audit event su Kafka.
 */
package io.secureflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
