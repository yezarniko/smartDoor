package com.smartdoor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class QrTokenServiceTest {
    private QrTokenService service;

    @BeforeEach
    void setUp() {
        service = new QrTokenService(new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(service, "secret", "test-secret-with-more-than-thirty-two-characters");
        ReflectionTestUtils.invokeMethod(service, "validateSecret");
    }

    @Test
    void createsAndVerifiesOpaqueToken() {
        String token = service.create("credential-id", "nonce", Instant.now(), Instant.now().plusSeconds(3600));
        assertEquals("credential-id", service.verify(token).cid());
        assertFalse(token.contains("credential-id"));
    }

    @Test
    void rejectsTampering() {
        String token = service.create("credential-id", "nonce", Instant.now(), Instant.now().plusSeconds(3600));
        assertThrows(IllegalArgumentException.class, () -> service.verify(token.substring(0, token.length() - 2) + "aa"));
    }
}

