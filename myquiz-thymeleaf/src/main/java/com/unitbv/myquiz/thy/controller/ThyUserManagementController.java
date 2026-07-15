package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thymeleaf controller for user management in admin interface
 */
@Controller
@RequestMapping(ControllerSettings.PATH_ADMIN_USERS)
public class ThyUserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ThyUserManagementController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${AUTH_API_URL}")
    private String authApiUrl;

    @Autowired
    public ThyUserManagementController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Get admin API base URL by replacing /api/auth with /api/admin
     */
    private String getAdminApiUrl() {
        return authApiUrl.replace("/api/auth", "/api/admin");
    }

    private String buildAdminUserUrl(Long userId) {
        return getAdminApiUrl() + ControllerSettings.API_ADMIN_USERS + "/" + userId;
    }

    /**
     * Validates session and permission required for user management actions.
     */
    private String requireManageUsersPermission() {
        String sessionCheck = sessionService.validateSessionOrRedirect();
        if (sessionCheck != null) return sessionCheck;
        if (!sessionService.hasAdminRole()) {
            logger.warn("Access denied: missing ADMIN role");
            return ControllerSettings.REDIRECT_ACCESS_DENIED;
        }
        if (!sessionService.hasPermission(ControllerSettings.PERMISSION_MODIFY_USER)) {
            logger.warn("Access denied: missing permission {}", ControllerSettings.PERMISSION_MODIFY_USER);
            return ControllerSettings.REDIRECT_ACCESS_DENIED;
        }
        return null;
    }

    private String buildUsersRedirectWithError(String errorMessage) {
        return UriComponentsBuilder.fromPath(ControllerSettings.PATH_ADMIN_USERS)
                .queryParam(ControllerSettings.ATTR_ERROR, errorMessage)
                .build()
                .toUriString()
                .replaceFirst("^", "redirect:");
    }

    private String buildUserEditRedirect(Long userId, String errorMessage) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath(ControllerSettings.PATH_ADMIN_USERS + "/" + userId + "/edit");
        if (errorMessage != null && !errorMessage.isBlank()) {
            builder.queryParam(ControllerSettings.ATTR_ERROR, errorMessage);
        }
        return "redirect:" + builder.build().toUriString();
    }

    /**
     * List all users
     */
    @GetMapping({"", "/"})
    public String listUsers(Model model) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Fetching user list");
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                getAdminApiUrl() + ControllerSettings.API_ADMIN_USERS,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            model.addAttribute(ControllerSettings.ATTR_USERS, response.getBody() != null ? response.getBody() : List.of());
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_ADMIN_USER_LIST;
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_FAILED_LOAD_USERS);
            model.addAttribute(ControllerSettings.ATTR_USERS, List.of());
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_ADMIN_USER_LIST;
        }
    }

    /**
     * Show user edit form
     */
    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Loading edit form for user {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();

            // Fetch user details
            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                buildAdminUserUrl(id),
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // Fetch all roles
            ResponseEntity<List<Map<String, Object>>> rolesResponse = restTemplate.exchange(
                getAdminApiUrl() + ControllerSettings.API_ADMIN_ROLES,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> user = userResponse.getBody();
            List<Map<String, Object>> allRoles = rolesResponse.getBody();

            // Get user's current role names
            @SuppressWarnings("unchecked")
            Set<String> userRoles = user != null && user.get("roleNames") != null
                ? new HashSet<>((Collection<String>) user.get("roleNames"))
                : new HashSet<>();

            model.addAttribute(ControllerSettings.ATTR_USER, user);
            model.addAttribute(ControllerSettings.ATTR_ALL_ROLES, allRoles != null ? allRoles : List.of());
            model.addAttribute(ControllerSettings.ATTR_USER_ROLES, userRoles);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());

            return ControllerSettings.VIEW_ADMIN_USER_EDIT;
        } catch (Exception e) {
            logger.error("Error loading user edit form: {}", e.getMessage());
            return buildUsersRedirectWithError(ControllerSettings.MSG_FAILED_LOAD_USER);
        }
    }

    /**
     * Update user enabled status
     */
    @PostMapping("/{id}/enabled")
    public String updateUserEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Updating user {} enabled status to {}", id, enabled);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            String endpoint = UriComponentsBuilder
                    .fromUriString(buildAdminUserUrl(id) + ControllerSettings.API_ADMIN_ENABLED_SUFFIX)
                    .queryParam("enabled", enabled)
                    .toUriString();
            restTemplate.exchange(
                endpoint,
                HttpMethod.PUT,
                request,
                Void.class
            );
            return ControllerSettings.REDIRECT_ADMIN_USERS;
        } catch (Exception e) {
            logger.error("Error updating user enabled status: {}", e.getMessage());
            return buildUsersRedirectWithError(ControllerSettings.MSG_FAILED_UPDATE_USER);
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{userId}/roles/assign")
    public String assignRole(@PathVariable Long userId, @RequestParam Long roleId) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Assigning role {} to user {}", roleId, userId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                buildAdminUserUrl(userId) + "/roles/" + roleId,
                HttpMethod.POST,
                request,
                Void.class
            );
            return buildUserEditRedirect(userId, null);
        } catch (Exception e) {
            logger.error("Error assigning role: {}", e.getMessage());
            return buildUserEditRedirect(userId, ControllerSettings.MSG_FAILED_ASSIGN_ROLE);
        }
    }

    /**
     * Remove role from user
     */
    @PostMapping("/{userId}/roles/{roleId}/remove")
    public String removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Removing role {} from user {}", roleId, userId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                buildAdminUserUrl(userId) + "/roles/" + roleId,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            return buildUserEditRedirect(userId, null);
        } catch (Exception e) {
            logger.error("Error removing role: {}", e.getMessage());
            return buildUserEditRedirect(userId, ControllerSettings.MSG_FAILED_REMOVE_ROLE);
        }
    }

    /**
     * Delete user
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        String denied = requireManageUsersPermission();
        if (denied != null) return denied;
        logger.info("Deleting user {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                buildAdminUserUrl(id),
                HttpMethod.DELETE,
                request,
                Void.class
            );
            return ControllerSettings.REDIRECT_ADMIN_USERS;
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return buildUsersRedirectWithError(ControllerSettings.MSG_FAILED_DELETE_USER);
        }
    }
}

