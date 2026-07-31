/*
 * CoreServiceApplication — Entry point del Core Service.
 *
 * Servizio Spring Boot per CRUD tenant, utenti, ruoli, permessi, API key. Usa Keycloak
 * per JWT, MySQL per persistenza, Hibernate Filter per tenant isolation. Espone REST API
 * su /api/* protette da RBAC con @RequiresPermission.
 */
package io.secureflow.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
