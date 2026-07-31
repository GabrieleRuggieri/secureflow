/*
 * TenantIsolationIntegrationTest — Verifica l'isolamento dati tra tenant.
 *
 * Crea dati in Tenant A, imposta TenantContext su B, verifica che le query non
 * restituiscano i dati di A. Richiede Docker (Testcontainers MySQL). Tag
 * "integration" per escluderlo da mvn test senza Docker.
 */
package io.secureflow.core;

import io.secureflow.core.entity.Permission;
import io.secureflow.core.entity.Role;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.User;
import io.secureflow.core.repository.PermissionRepository;
import io.secureflow.core.repository.RoleRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.UserRepository;
import io.secureflow.core.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Hibernate tenant filter correctly isolates data between tenants.
 * Data created in Tenant A must not be visible when TenantContext is set to Tenant B.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Import(TenantIsolationIntegrationTest.TestSecurityConfig.class)
class TenantIsolationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("secureflow")
            .withUsername("secureflow")
            .withPassword("secureflow")
            .withReuse(true);

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PermissionRepository permissionRepository;

    Tenant tenantA;
    Tenant tenantB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantRepository.deleteAll();

        tenantA = new Tenant();
        tenantA.setName("Tenant A");
        tenantA.setSlug("tenant-a");
        tenantA.setRateLimitPerMinute(100);
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("Tenant B");
        tenantB.setSlug("tenant-b");
        tenantB.setRateLimitPerMinute(200);
        tenantB = tenantRepository.save(tenantB);
    }

    @Test
    void tenantFilter_isolatesUsers() {
        // Create user in Tenant A (no filter - direct save)
        User userA = new User();
        userA.setTenant(tenantA);
        userA.setKeycloakUserId("user-a-1");
        userA.setEmail("userA@tenant-a.local");
        userA.setUsername("userA");
        userRepository.save(userA);

        // With Tenant A context: should see user
        TenantContext.setTenantId(tenantA.getId());
        List<User> usersA = userRepository.findAll();
        assertThat(usersA).hasSize(1);
        assertThat(usersA.get(0).getUsername()).isEqualTo("userA");

        // With Tenant B context: should NOT see Tenant A's user
        TenantContext.setTenantId(tenantB.getId());
        List<User> usersB = userRepository.findAll();
        assertThat(usersB).isEmpty();

        TenantContext.clear();
    }

    @Test
    void tenantFilter_isolatesRoles() {
        // Create role in Tenant A
        Role roleA = new Role();
        roleA.setTenant(tenantA);
        roleA.setName("admin");
        roleA.setDescription("Admin role");
        roleRepository.save(roleA);

        TenantContext.setTenantId(tenantA.getId());
        assertThat(roleRepository.findAll()).hasSize(1);

        TenantContext.setTenantId(tenantB.getId());
        assertThat(roleRepository.findAll()).isEmpty();

        TenantContext.clear();
    }

    @Test
    void tenantFilter_isolatesPermissions() {
        Permission permA = new Permission();
        permA.setTenant(tenantA);
        permA.setResource("test");
        permA.setAction("read");
        permissionRepository.save(permA);

        TenantContext.setTenantId(tenantA.getId());
        assertThat(permissionRepository.findAll()).hasSize(1);

        TenantContext.setTenantId(tenantB.getId());
        assertThat(permissionRepository.findAll()).isEmpty();

        TenantContext.clear();
    }

    @Configuration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue("test")
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("tenantId", "default-tenant")
                    .build();
        }
    }
}
