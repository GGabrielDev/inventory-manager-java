package com.inventorymanager.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void createAndParseToken() {
        JwtService jwtService = new JwtService("change-me-please-change-me-please-change-me", 60);

        String token = jwtService.createToken(42L, "admin");
        Claims claims = jwtService.parse(token);

        assertNotNull(token);
        assertEquals("admin", claims.getSubject());
        assertEquals(42L, claims.get("userId", Long.class));
    }
}
