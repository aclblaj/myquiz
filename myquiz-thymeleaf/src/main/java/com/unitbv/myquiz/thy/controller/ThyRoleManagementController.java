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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thymeleaf controller for role management in admin interface
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
@RequestMapping("/admin/roles")
public class ThyRoleManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ThyRoleManagementController.class);
    private static final String PERMISSION_MANAGE_ROLES = "MODIFY_ROLE";
    private static final String REDIRECT_NO_PERMISSION = "redirect:/?error=Access+denied";
    private final RestTemplate restTemplate;
    private final SessionService sessionService;
    @Value("${AUTH_API_URL}")
    private String authApiUrl;

    @Autowired
    public ThyRoleManagementController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Get admin API base URL by replacing /api/auth with /api/admin
     */
    private String getAdminApiUrl() {
        return authApiUrl.replace("/api/auth", "/api/admin");
    }

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
     * List all roles
     */
    @GetMapping({"", "/"})
    public String listRoles(Model model) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Fetching role list");
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    getAdminApiUrl() + "/roles", HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );

            model.addAttribute(ControllerSettings.ATTR_ROLES, response.getBody());
            return "admin/role-list";
        } catch (Exception e) {
            logger.error("Error fetching roles: {}", e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_ERROR, "Failed to load roles");
            return "admin/role-list";
        }
    }

    /**
     * Show role add form
     */
    @GetMapping("/new")
    public String newRoleForm(Model model) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Loading new role form");
        model.addAttribute(ControllerSettings.ATTR_ROLE, new HashMap<String, String>());
        return "admin/role-add";
    }

    /**
     * Create new role
     */
    @PostMapping("/new")
    public String createRole(@RequestParam String name, @RequestParam String description) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Creating new role: {}", name);
        try {
            Map<String, String> roleData = new HashMap<>();
            roleData.put("name", name);
            roleData.put("description", description);

            HttpEntity<Map<String, String>> request = sessionService.createAuthorizedRequest(roleData);
            restTemplate.exchange(getAdminApiUrl() + "/roles", HttpMethod.POST, request, Map.class);
            return "redirect:/admin/roles";
        } catch (Exception e) {
            logger.error("Error creating role: {}", e.getMessage());
            return "redirect:/admin/roles/new?error=Failed+to+create+role";
        }
    }

    /**
     * Show role edit form
     */
    @GetMapping("/{id}/edit")
    public String editRoleForm(@PathVariable Long id, Model model) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Loading edit form for role {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();

            // Fetch role details
            ResponseEntity<Map<String, Object>> roleResponse = restTemplate.exchange(
                    getAdminApiUrl() + "/roles/" + id, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );

            // Fetch all permissions
            ResponseEntity<List<Map<String, Object>>> permissionsResponse = restTemplate.exchange(
                    getAdminApiUrl() + "/permissions", HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );

            Map<String, Object> role = roleResponse.getBody();
            List<Map<String, Object>> allPermissions = permissionsResponse.getBody();

            // Get role's current permission names
            @SuppressWarnings("unchecked") Set<String> rolePermissions = role != null && role.get("permissionNames") != null ? new HashSet<>(
                    (Collection<String>) role.get("permissionNames")) : new HashSet<>();

            model.addAttribute(ControllerSettings.ATTR_ROLE, role);
            model.addAttribute("allPermissions", allPermissions);
            model.addAttribute("rolePermissions", rolePermissions);

            return "admin/role-edit";
        } catch (Exception e) {
            logger.error("Error loading role edit form: {}", e.getMessage());
            return "redirect:/admin/roles?error=Failed+to+load+role";
        }
    }

    /**
     * Update role
     */
    @PostMapping("/{id}/update")
    public String updateRole(@PathVariable Long id, @RequestParam String name, @RequestParam String description) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Updating role {}", id);
        try {
            Map<String, String> roleData = new HashMap<>();
            roleData.put("name", name);
            roleData.put("description", description);

            HttpEntity<Map<String, String>> request = sessionService.createAuthorizedRequest(roleData);
            restTemplate.exchange(getAdminApiUrl() + "/roles/" + id, HttpMethod.PUT, request, Void.class);
            return "redirect:/admin/roles";
        } catch (Exception e) {
            logger.error("Error updating role: {}", e.getMessage());
            return "redirect:/admin/roles/" + id + "/edit?error=Failed+to+update+role";
        }
    }

    /**
     * Assign permission to role
     */
    @PostMapping("/{roleId}/permissions/assign")
    public String assignPermission(@PathVariable Long roleId, @RequestParam Long permissionId) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Assigning permission {} to role {}", permissionId, roleId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(getAdminApiUrl() + "/roles/" + roleId + "/permissions/" + permissionId, HttpMethod.POST, request, Void.class);
            return "redirect:/admin/roles/" + roleId + "/edit";
        } catch (Exception e) {
            logger.error("Error assigning permission: {}", e.getMessage());
            return "redirect:/admin/roles/" + roleId + "/edit?error=Failed+to+assign+permission";
        }
    }

    /**
     * Remove permission from role
     */
    @PostMapping("/{roleId}/permissions/{permissionId}/remove")
    public String removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Removing permission {} from role {}", permissionId, roleId);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(getAdminApiUrl() + "/roles/" + roleId + "/permissions/" + permissionId, HttpMethod.DELETE, request, Void.class);
            return "redirect:/admin/roles/" + roleId + "/edit";
        } catch (Exception e) {
            logger.error("Error removing permission: {}", e.getMessage());
            return "redirect:/admin/roles/" + roleId + "/edit?error=Failed+to+remove+permission";
        }
    }

    /**
     * Delete role
     */
    @PostMapping("/{id}/delete")
    public String deleteRole(@PathVariable Long id) {
        String denied = requirePermission(PERMISSION_MANAGE_ROLES);
        if (denied != null) return denied;
        logger.info("Deleting role {}", id);
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(getAdminApiUrl() + "/roles/" + id, HttpMethod.DELETE, request, Void.class);
            return "redirect:/admin/roles";
        } catch (Exception e) {
            logger.error("Error deleting role: {}", e.getMessage());
            return "redirect:/admin/roles?error=Failed+to+delete+role";
        }
    }
}

