package com.smartdoor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SmartDoorApplicationIntegrationTest {
    @Container
    static MariaDBContainer database = new MariaDBContainer("mariadb:11.4")
            .withDatabaseName("smartdoor").withUsername("smartdoor").withPassword("smartdoor");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
        registry.add("smartdoor.qr.secret", () -> "integration-test-secret-with-more-than-32-characters");
        registry.add("smartdoor.mqtt.port", () -> 65530);
    }

    @Test void contextLoads() {}
}
