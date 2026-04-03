package com.gomoku.sync.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * 静默登录后签发的会话 JWT，用于 HTTP 与 WebSocket 识别真实用户（user id）。
 */
@Service
public class SessionJwtService {

    private static final long EXPIRY_MS = 30L * 24 * 60 * 60 * 1000;

    private final SecretKey key;

    public SessionJwtService(
            @Value("${gomoku.jwt.secret:gomoku-dev-only-change-me-in-production}") String secret) {
        this.key = keyFromSecret(secret);
    }

    private static SecretKey keyFromSecret(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            raw = md.digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        return Keys.hmacShaKeyFor(raw);
    }

    public String createToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(EXPIRY_MS)))
                .signWith(key)
                .compact();
    }

    public Optional<Long> parseUserId(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String sub = claims.getSubject();
            if (sub == null) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(sub));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 解析 {@code Authorization: Bearer &lt;token&gt;}。
     */
    public Optional<Long> parseAuthorizationBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return parseUserId(authorization.substring(7).trim());
    }
}
