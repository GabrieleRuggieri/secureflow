/*
 * AsyncConfig — Abilita scheduling per il webhook delivery worker.
 *
 * RestClient.Builder è auto-configurato da Spring Boot 3.2+.
 */
package io.secureflow.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AsyncConfig {
}
