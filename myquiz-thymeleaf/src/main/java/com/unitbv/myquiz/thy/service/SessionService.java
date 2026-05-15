package com.unitbv.myquiz.thy.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing user sessions and authorization in a thread-safe manner.
 * <p>
 * This service retrieves the current HTTP session from the request context for each operation,
 * ensuring thread-safety in multi-user environments. It provides methods for:
 * <ul>
 *   <li>Session validation</li>
 *   <li>JWT token management</li>
 *   <li>Authorization header creation</li>
 *   <li>Session invalidation</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong> All methods retrieve the session from RequestContextHolder
 * on each invocation, making this service safe for concurrent use by multiple requests.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Retrieves the current HTTP session from the request context.
     * <p>
     * This method is called internally by all session operations to ensure
     * thread-safe access to the current request's session.
     *
     * @param create whether to create a new session if one doesn't exist
     * @return the current HTTP session, or null if not available
     */
    private HttpSession getCurrentSession(boolean create) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getSession(create);
        }
        return null;
    }

    /**
     * Retrieves the current HTTP session (creates if doesn't exist).
     *
     * @return the current HTTP session, or null if not in a request context
     */
    private HttpSession getCurrentSession() {
        return getCurrentSession(true);
    }

    /**
     * Validates if the session contains all required authentication variables.
     *
     * @return true if session is valid (has user and valid JWT token), false otherwise
     */
    public boolean containsValidVars() {
        HttpSession currentSession = getCurrentSession(false);
        boolean isValid = currentSession != null &&
               getLoggedInUser() != null &&
               getJwtToken() != null &&
               !getJwtToken().isBlank();

        if (!isValid) {
            log.atWarn().setMessage("Session validation failed - session exists: {}, user exists: {}, token valid: {}")
                .addArgument(currentSession != null)
                .addArgument(getLoggedInUser() != null)
                .addArgument(getJwtToken() != null && !getJwtToken().isBlank())
                .log();
        }

        return isValid;
    }

    /**
     * Validates session and returns redirect view if invalid.
     *
     * @return null if session is valid, redirect view string if invalid
     */
    public String validateSessionOrRedirect() {
        if (!containsValidVars()) {
            log.atWarn().log("Session validation failed, redirecting to login");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        return null;
    }

    /**
     * Creates an HttpEntity with Authorization header (no body).
     *
     * @return HttpEntity with authorization header, or null if token is invalid
     */
    public HttpEntity<Void> getAuthorizationHeader() {
        String jwtToken = getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            log.atWarn().log("Cannot create authorization header: JWT token is null or blank");
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(ControllerSettings.HEADER_AUTHORIZATION, ControllerSettings.BEARER_PREFIX + jwtToken);
        return new HttpEntity<>(headers);
    }

    /**
     * Creates an HttpEntity with Authorization header and JSON content type.
     *
     * @param <T> the type of the request body
     * @param body the request body
     * @return HttpEntity with authorization header and body
     */
    public <T> HttpEntity<T> createAuthorizedRequest(T body) {
        HttpHeaders headers = createAuthHeaders();
        return new HttpEntity<>(body, headers);
    }

    /**
     * Creates an HttpEntity with Authorization header, JSON content type, and no body.
     *
     * @return HttpEntity with authorization header
     */
    public HttpEntity<Void> createAuthorizedRequest() {
        HttpHeaders headers = createAuthHeaders();
        return new HttpEntity<>(headers);
    }

    /**
     * Creates HTTP headers with Authorization and Content-Type: application/json.
     *
     * @return HttpHeaders with authorization and content type
     * @throws IllegalStateException if JWT token is missing or blank
     */
    public HttpHeaders createAuthHeaders() {
        String jwtToken = getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            log.atWarn().log("Cannot create auth headers: JWT token is null or blank");
            throw new IllegalStateException("Session expired or missing token. Please log in again.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(ControllerSettings.HEADER_AUTHORIZATION, ControllerSettings.BEARER_PREFIX + jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Creates an HttpEntity for multipart file uploads with authorization headers.
     * Sets Content-Type to multipart/form-data and includes Authorization header.
     *
     * @param <T> the type of the request body
     * @param body the multipart body (typically MultiValueMap)
     * @return HttpEntity with multipart headers and authorization
     */
    public <T> HttpEntity<T> createMultipartRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Get authorization header from existing method
        HttpEntity<Void> authEntity = getAuthorizationHeader();
        if (authEntity != null && authEntity.getHeaders().get(ControllerSettings.HEADER_AUTHORIZATION) != null) {
            headers.put(ControllerSettings.HEADER_AUTHORIZATION,
                authEntity.getHeaders().get(ControllerSettings.HEADER_AUTHORIZATION));
        }

        return new HttpEntity<>(body, headers);
    }

    /**
     * Invalidates the current session retrieved from request context.
     * This method is thread-safe and works correctly in multi-user environments.
     */
    public void invalidateCurrentSession() {
        HttpSession currentSession = getCurrentSession(false);
        if (currentSession != null) {
            invalidateSession(currentSession);
        } else {
            log.atDebug().log("No session to invalidate in current request context");
        }
    }

    /**
     * Invalidates the specified session.
     *
     * @param session the session to invalidate
     */
    public void invalidateSession(HttpSession session) {
        if (session != null) {
            try {
                String sessionId = session.getId();
                session.invalidate();
                log.atInfo().setMessage("Invalidated session ID: {}").addArgument(sessionId).log();
            } catch (IllegalStateException e) {
                log.atWarn().setMessage("Session already invalidated: {}").addArgument(e.getMessage()).log();
            }
        }
    }

    /**
     * Gets the current HTTP session.
     * <p>
     * <strong>Note:</strong> This method retrieves the session from the current request context
     * each time it's called, ensuring thread-safety.
     *
     * @return the current HTTP session, or null if not in a request context
     */
    public HttpSession getSession() {
        return getCurrentSession();
    }

    /**
     * Gets the logged-in user from the current session.
     *
     * @return the logged-in user object, or null if not found or session is invalid
     */
    public Object getLoggedInUser() {
        HttpSession currentSession = getCurrentSession(false);
        if (currentSession != null) {
            try {
                return currentSession.getAttribute(ControllerSettings.ATTR_LOGGED_IN_USER);
            } catch (IllegalStateException e) {
                log.atDebug().setMessage("Session already invalidated when getting logged-in user").log();
                return null;
            }
        } else {
            log.atDebug().log("No session available when trying to get logged-in user");
            return null;
        }
    }

    /**
     * Gets the JWT token from the current session.
     *
     * @return the JWT token string, or null if not found or session is invalid
     */
    public String getJwtToken() {
        HttpSession currentSession = getCurrentSession(false);
        if (currentSession != null) {
            try {
                return (String) currentSession.getAttribute(ControllerSettings.ATTR_JWT_TOKEN);
            } catch (IllegalStateException e) {
                log.atDebug().setMessage("Session already invalidated when getting JWT token").log();
                return null;
            }
        } else {
            log.atDebug().log("No session available when trying to get JWT token");
            return null;
        }
    }

    /**
     * Extracts the set of permissions from the JWT token in the current session.
     *
     * @return Set of permission names, empty if none
     */
    public Set<String> getPermissionsFromToken() {
        try {
            String token = getJwtToken();
            if (token == null || token.isBlank()) return new HashSet<>();
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            List<String> perms = claims.get("permissions", List.class);
            return perms != null ? new HashSet<>(perms) : new HashSet<>();
        } catch (Exception e) {
            log.warn("Failed to extract permissions from JWT: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Extracts the set of role names from the JWT token in the current session.
     *
     * @return Set of role names, empty if none
     */
    public Set<String> getRolesFromToken() {
        try {
            String token = getJwtToken();
            if (token == null || token.isBlank()) return new HashSet<>();
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            List<String> roles = claims.get("roles", List.class);
            return roles != null ? new HashSet<>(roles) : new HashSet<>();
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Checks if the current session user has the given permission.
     *
     * @param permission Permission name to check
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        return getPermissionsFromToken().contains(permission);
    }

    /**
     * Checks if the current session user has the given role name.
     *
     * @param role Role name to check (e.g. ADMIN)
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return getRolesFromToken().contains(role);
    }

    /**
     *
     * Checks if the current session user administrative role.
     * @return true if user has "ADMIN" or "ADMINISTRATOR" role, false otherwise
     */
    public boolean hasAdminRole() {
        return hasRole("ADMIN") || hasRole("ADMINISTRATOR");
    }

}
