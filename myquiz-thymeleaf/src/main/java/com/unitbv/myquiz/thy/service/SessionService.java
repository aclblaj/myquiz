package com.unitbv.myquiz.thy.service;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for managing user sessions and authorization.
 * Provides reusable methods for session validation and HTTP header creation.
 */
@Service
public class SessionService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    HttpSession session;

    public SessionService() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            this.session = attr.getRequest().getSession(true);
            log.info("SessionService initialized with session ID: {}", this.session.getId());
        }
    }

    /**
     * Validates if the session contains all required authentication variables.
     * Logs a warning if validation fails.
     *
     * @return true if session is valid, false otherwise
     */
    public boolean containsValidVars() {
        boolean isValid = this.session != null &&
               this.getLoggedInUser() != null &&
               this.getJwtToken() != null &&
               !this.getJwtToken().isBlank();

        if (!isValid) {
            log.warn("Session validation failed - session: {}, user: {}, token present: {}",
                this.session != null,
                this.getLoggedInUser() != null,
                this.getJwtToken() != null && !this.getJwtToken().isBlank());
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
            log.warn("Session validation failed, redirecting to login");
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
            log.warn("Cannot create authorization header: JWT token is null or blank");
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
     */
    public HttpHeaders createAuthHeaders() {
        String jwtToken = getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            log.warn("Cannot create auth headers: JWT token is null or blank");
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
     * Invalidates the current session.
     */
    public void invalidateCurrentSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpSession currentSession = attrs.getRequest().getSession(false);
            invalidateSession(currentSession);
        } else if (this.session != null) {
            invalidateSession(this.session);
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
                log.info("Invalidating session ID: {}", session.getId());
                session.invalidate();
            } catch (IllegalStateException e) {
                log.warn("Session already invalidated: {}", e.getMessage());
            }
        }
    }

    public HttpSession getSession() {
        return this.session;
    }

    public Object getLoggedInUser() {
        if (this.session != null) {
            try {
                return this.session.getAttribute(ControllerSettings.ATTR_LOGGED_IN_USER);
            } catch (IllegalStateException e) {
                log.debug("Session already invalidated when getting logged-in user");
                return null;
            }
        } else {
            log.warn("Session is null when trying to get logged-in user");
            return null;
        }
    }

    public String getJwtToken() {
        if (this.session != null) {
            try {
                return (String) this.session.getAttribute(ControllerSettings.ATTR_JWT_TOKEN);
            } catch (IllegalStateException e) {
                log.debug("Session already invalidated when getting JWT token");
                return null;
            }
        } else {
            log.warn("Session is null when trying to get JWT token");
            return null;
        }
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }
}
