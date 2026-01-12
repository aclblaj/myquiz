package com.unitbv.myquiz.app.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    @Value("${jwt.secret}")
    private String jwtSecret;
    private SecretKey getSecretKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        if (jwtSecret.length() < 32) {
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
        log.debug("[JwtUtil] Extracting username from token");
        try {
            String username = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.debug("[JwtUtil] Extracted username: {}", username);
            return username;
        } catch (Exception e) {
            log.error("[JwtUtil] Failed to extract username from token due to {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract username from token", e);
        }
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        log.debug("[JwtUtil] Validating token for user: {}", userDetails.getUsername());
        try {
            String username = extractUsername(token);
            boolean expired = isTokenExpired(token);
            boolean valid = username.equals(userDetails.getUsername()) && !expired;
            log.debug("[JwtUtil] Username match: {}, Token expired: {}, Validation result: {}", username.equals(userDetails.getUsername()), expired, valid);
            return valid;
        } catch (Exception e) {
            log.error("[JwtUtil] Token validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate JWT token without UserDetails - checks signature and expiration only
     * @param token JWT token to validate
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        log.debug("[JwtUtil] Validating token (signature and expiration only)");
        try {
            // This will throw an exception if signature is invalid
            extractUsername(token);
            boolean expired = isTokenExpired(token);
            boolean valid = !expired;
            log.debug("[JwtUtil] Token expired: {}, Validation result: {}", expired, valid);
            return valid;
        } catch (Exception e) {
            log.error("[JwtUtil] Token validation failed: {}", e.getMessage(), e);
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
            boolean expired = expiration.before(new Date());
            log.debug("[JwtUtil] Token expiration: {}, Is expired: {}", expiration, expired);
            return expired;
        } catch (Exception e) {
            log.error("[JwtUtil] Failed to check token expiration: {}", e.getMessage(), e);
            return true;
        }
    }
}
