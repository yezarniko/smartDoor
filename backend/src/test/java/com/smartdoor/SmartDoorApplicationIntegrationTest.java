package com.smartdoor;

import com.smartdoor.api.dto.ApiDtos.RoleRequest;
import com.smartdoor.api.dto.ApiDtos.UserCreateRequest;
import com.smartdoor.api.dto.ApiDtos.UserResponse;
import com.smartdoor.domain.Enums.UserRole;
import com.smartdoor.service.RoleService;
import com.smartdoor.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Autowired UserService users;
    @Autowired RoleService roles;

    @Test void contextLoads() {}

    @Test
    void serverAssignsSmallestAvailableCodeAndProtectsAssignedRoles() {
        roles.create(new RoleRequest("PHARMACIST", "Pharmacist", UserRole.STAFF));
        UserResponse first = users.create(new UserCreateRequest("First User", null, null, "PHARMACIST"));
        UserResponse second = users.create(new UserCreateRequest("Second User", null, null, "PHARMACIST"));
        assertEquals(1, first.code());
        assertEquals(2, second.code());
        assertThrows(IllegalArgumentException.class, () -> roles.delete("PHARMACIST"));

        users.delete(first.id());
        UserResponse replacement = users.create(new UserCreateRequest("Replacement User", null, null, "PHARMACIST"));
        assertEquals(1, replacement.code());

        users.delete(second.id());
        users.delete(replacement.id());
        roles.delete("PHARMACIST");
    }
}
