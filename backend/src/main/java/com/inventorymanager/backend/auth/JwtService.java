package com.inventorymanager.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expireMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expire-minutes:60}") long expireMinutes
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] expanded = new byte[32];
            for (int i = 0; i < 32; i++) {
                expanded[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = expanded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expireMinutes = expireMinutes;
    }

    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
