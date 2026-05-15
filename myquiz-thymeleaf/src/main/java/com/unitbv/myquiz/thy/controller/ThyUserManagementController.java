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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Thymeleaf controller for user management in admin interface
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
@RequestMapping("/admin/users")
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

    private static final String PERMISSION_MANAGE_USERS = "MODIFY_USER";
    private static final String REDIRECT_NO_PERMISSION = "redirect:/?error=Access+denied";

    private String requirePermission(String permission) {
        String sessionCheck = sessionService.validateSessionOrRedirect();
        if (sessionCheck != null) return sessionCheck;
        if (!sessionService.hasAdminRole()) {
            logger.warn("Access denied: missing ADMIN role");
            return REDIRECT_NO_PERMISSION;
        }
        if (!sessionService.hasPermission(permission)) {
            logger.warn("Access denied: missing permission {}", permission);
            return REDIRECT_NO_PERMISSION;
        }
        return null;
    }

    /**
     * List all users
     */
    @GetMapping({"", "/"})
    public String listUsers(Model model) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Fetching user list");
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                getAdminApiUrl() + "/users",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            model.addAttribute("users", response.getBody());
            return "admin/user-list";
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_ERROR, "Failed to load users");
            return "admin/user-list";
        }
    }

    /**
     * Show user edit form
     */
    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Loading edit form for user {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();

            // Fetch user details
            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                getAdminApiUrl() + "/users/" + id,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Fetch all roles
            ResponseEntity<List<Map<String, Object>>> rolesResponse = restTemplate.exchange(
                getAdminApiUrl() + "/roles",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, Object> user = userResponse.getBody();
            List<Map<String, Object>> allRoles = rolesResponse.getBody();

            // Get user's current role names
            @SuppressWarnings("unchecked")
            Set<String> userRoles = user != null && user.get("roleNames") != null
                ? new HashSet<>((Collection<String>) user.get("roleNames"))
                : new HashSet<>();

            model.addAttribute("user", user);
            model.addAttribute("allRoles", allRoles);
            model.addAttribute("userRoles", userRoles);

            return "admin/user-edit";
        } catch (Exception e) {
            logger.error("Error loading user edit form: {}", e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_ERROR, "Failed to load user");
            return "redirect:/admin/users";
        }
    }

    /**
     * Update user enabled status
     */
    @PostMapping("/{id}/enabled")
    public String updateUserEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Updating user {} enabled status to {}", id, enabled);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                getAdminApiUrl() + "/users/" + id + "/enabled?enabled=" + enabled,
                HttpMethod.PUT,
                request,
                Void.class
            );
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.error("Error updating user enabled status: {}", e.getMessage());
            return "redirect:/admin/users?error=Failed+to+update+user";
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{userId}/roles/assign")
    public String assignRole(@PathVariable Long userId, @RequestParam Long roleId) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Assigning role {} to user {}", roleId, userId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                getAdminApiUrl() + "/users/" + userId + "/roles/" + roleId,
                HttpMethod.POST,
                request,
                Void.class
            );
            return "redirect:/admin/users/" + userId + "/edit";
        } catch (Exception e) {
            logger.error("Error assigning role: {}", e.getMessage());
            return "redirect:/admin/users/" + userId + "/edit?error=Failed+to+assign+role";
        }
    }

    /**
     * Remove role from user
     */
    @PostMapping("/{userId}/roles/{roleId}/remove")
    public String removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Removing role {} from user {}", roleId, userId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                getAdminApiUrl() + "/users/" + userId + "/roles/" + roleId,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            return "redirect:/admin/users/" + userId + "/edit";
        } catch (Exception e) {
            logger.error("Error removing role: {}", e.getMessage());
            return "redirect:/admin/users/" + userId + "/edit?error=Failed+to+remove+role";
        }
    }

    /**
     * Delete user
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        String denied = requirePermission(PERMISSION_MANAGE_USERS);
        if (denied != null) return denied;
        logger.info("Deleting user {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(
                getAdminApiUrl() + "/users/" + id,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return "redirect:/admin/users?error=Failed+to+delete+user";
        }
    }
}

