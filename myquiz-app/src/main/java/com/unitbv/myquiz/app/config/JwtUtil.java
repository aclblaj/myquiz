package com.unitbv.myquiz.app.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
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
            log.atWarn().addArgument(jwtSecret.length()).log("[JwtUtil] Provided jwt.secret may be too short (length={}), consider using at least 32+ characters.");
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
        log.atDebug().log("[JwtUtil] Extracting username from token");
        try {
            String username = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.atDebug().addArgument(username).log("[JwtUtil] Extracted username: {}");
            return username;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("[JwtUtil] Failed to extract username from token due to {}");
            throw new JwtException("Failed to extract username from token", e);
        }
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        log.atDebug().addArgument(userDetails.getUsername()).log("[JwtUtil] Validating token for user: {}");
        try {
            String username = extractUsername(token);
            boolean expired = isTokenExpired(token);
            boolean valid = username.equals(userDetails.getUsername()) && !expired;
            log.atDebug().addArgument(username.equals(userDetails.getUsername())).addArgument(expired).addArgument(valid).log("[JwtUtil] Username match: {}, Token expired: {}, Validation result: {}");
            return valid;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("[JwtUtil] Token validation failed: {}");
            return false;
        }
    }

    /**
     * Validate JWT token without UserDetails - checks signature and expiration only
     * @param token JWT token to validate
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        log.atDebug().log("[JwtUtil] Validating token (signature and expiration only)");
        try {
            // This will throw an exception if signature is invalid
            extractUsername(token);
            boolean expired = isTokenExpired(token);
            boolean valid = !expired;
            log.atDebug().addArgument(expired).addArgument(valid).log("[JwtUtil] Token expired: {}, Validation result: {}");
            return valid;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("[JwtUtil] Token validation failed: {}");
            return false;
        }
    }
    /**
     * Extract roles from JWT token
     * @param token JWT token
     * @return Set of role names
     */
    public Set<String> extractRoles(String token) {
        log.atDebug().log("[JwtUtil] Extracting roles from token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<String> rolesList = claims.get("roles", List.class);
            if (rolesList != null) {
                Set<String> roles = new HashSet<>(rolesList);
                log.atDebug().addArgument(roles.size()).log("[JwtUtil] Extracted {} roles");
                return roles;
            }
            log.atDebug().log("[JwtUtil] No roles found in token");
            return new HashSet<>();
        } catch (Exception e) {
            log.atError().addArgument(e.getMessage()).log("[JwtUtil] Failed to extract roles from token: {}");
            return new HashSet<>();
        }
    }

    /**
     * Extract permissions from JWT token
     * @param token JWT token
     * @return Set of permission names
     */
    public Set<String> extractPermissions(String token) {
        log.atDebug().log("[JwtUtil] Extracting permissions from token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<String> permissionsList = claims.get("permissions", List.class);
            if (permissionsList != null) {
                Set<String> permissions = new HashSet<>(permissionsList);
                log.atDebug().addArgument(permissions.size()).log("[JwtUtil] Extracted {} permissions");
                return permissions;
            }
            log.atDebug().log("[JwtUtil] No permissions found in token");
            return new HashSet<>();
        } catch (Exception e) {
            log.atError().addArgument(e.getMessage()).log("[JwtUtil] Failed to extract permissions from token: {}");
            return new HashSet<>();
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
            log.atDebug().addArgument(expiration).addArgument(expired).log("[JwtUtil] Token expiration: {}, Is expired: {}");
            return expired;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("[JwtUtil] Failed to check token expiration: {}");
            return true;
        }
    }
}
