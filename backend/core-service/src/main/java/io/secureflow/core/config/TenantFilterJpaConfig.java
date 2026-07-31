/*
 * TenantFilterJpaConfig — Wrappa l'EntityManagerFactory nativo per abilitare il filtro tenant.
 *
 * Usa un BeanPostProcessor sul bean "entityManagerFactory" invece di un secondo @Primary EMF:
 * definire un EMF aggiuntivo in Boot 3.4 lascia Spring Data senza il bean nominato
 * entityManagerFactory (fallimento a runtime in Docker).
 */
package io.secureflow.core.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class TenantFilterJpaConfig {

    @Bean
    static BeanPostProcessor tenantFilterEntityManagerFactoryPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
                    throws BeansException {
                if ("entityManagerFactory".equals(beanName)
                        && bean instanceof EntityManagerFactory emf
                        && !(bean instanceof TenantFilterEntityManagerFactory)) {
                    return new TenantFilterEntityManagerFactory(emf);
                }
                return bean;
            }
        };
    }
}
