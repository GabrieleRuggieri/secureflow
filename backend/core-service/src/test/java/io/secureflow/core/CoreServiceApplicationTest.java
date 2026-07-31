/*
 * CoreServiceApplicationTest — Test di caricamento del contesto Spring.
 *
 * Verifica che l'applicazione si avvii senza errori. Usa profile "test" con H2
 * in-memory. I test di integrazione con Testcontainers sono in TenantIsolationIntegrationTest
 * (tag "integration", esclusi di default).
 */
package io.secureflow.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoreServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
