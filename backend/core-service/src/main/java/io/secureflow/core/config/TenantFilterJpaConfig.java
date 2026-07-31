/*
 * TenantFilterJpaConfig — Wrapper dell'EntityManagerFactory per il filtro tenant.
 *
 * Crea un bean @Primary che wrappa il LocalContainerEntityManagerFactoryBean nativo.
 * Ogni EntityManager creato avrà il filtro "tenantFilter" abilitato con tenantId da
 * TenantContext (se impostato). Disabilitato in profile "test" per evitare dipendenze
 * circolari con l'EMF di test.
 */
package io.secureflow.core.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class TenantFilterJpaConfig {

    /**
     * Wraps the native EntityManagerFactory to enable the tenant filter on each EntityManager.
     * The filter is enabled with the tenant ID from TenantContext (set by TenantContextFilter).
     * Depends on LocalContainerEntityManagerFactoryBean to avoid circular dependency.
     */
    @Bean("filteringEntityManagerFactory")
    @Primary
    public EntityManagerFactory filteringEntityManagerFactory(
            @Qualifier("&entityManagerFactory") LocalContainerEntityManagerFactoryBean emfBean) {
        return new TenantFilterEntityManagerFactory(emfBean.getObject());
    }
}
