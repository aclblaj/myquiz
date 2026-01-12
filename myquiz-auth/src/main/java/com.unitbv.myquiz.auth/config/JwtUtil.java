package com.unitbv.myquiz.auth.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    @Value("${jwt.secret}")
    private String jwtSecret;
    private SecretKey getSecretKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        if (jwtSecret.length() < 32) { // 256 bits ~ 32 bytes
            log.warn("[JwtUtil] Provided jwt.secret may be too short (length={}), consider using at least 32+ characters.", jwtSecret.length());
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSecretKey())
                .compact();
    }
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            log.error("[JwtUtil] Failed to extract username from token: {}", e.getMessage(), e);
            throw e;
        }
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String extracted = extractUsername(token);
            boolean expired = isTokenExpired(token);
            boolean valid = extracted.equals(userDetails.getUsername()) && !expired;
            log.debug("[JwtUtil] Validation result for user {}: {} (expired={})", userDetails.getUsername(), valid, expired);
            return valid;
        } catch (Exception e) {
            log.error("[JwtUtil] Token validation error: {}", e.getMessage());
            return false;
        }
    }
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("[JwtUtil] Failed to determine token expiration: {}", e.getMessage());
            return true; // treat as expired on failure
        }
    }
}
