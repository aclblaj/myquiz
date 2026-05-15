package com.unitbv.myquiz.auth.controller;

import com.unitbv.myquiz.auth.dto.AdminRoleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * REST Controller for role management in admin interface
 */
@RestController
@RequestMapping("/api/admin/roles")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class RoleManagementController {

    private static final Logger logger = LoggerFactory.getLogger(RoleManagementController.class);

    private final RestTemplate restTemplate;
    private final String iamUrl;

    public RoleManagementController(RestTemplate restTemplate,
                                   @Value("${MYQUIZ_IAM_URL}") String iamUrl) {
        this.restTemplate = restTemplate;
        this.iamUrl = iamUrl;
    }

    /**
     * Get all roles with their permissions
     */
    @GetMapping
    public ResponseEntity<List<AdminRoleDto>> getAllRoles() {
        try {
            logger.info("Fetching all roles from IAM");
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                iamUrl + "/roles", Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<AdminRoleDto> roles = new ArrayList<>();
                for (Object roleObj : response.getBody()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> roleMap = (Map<String, Object>) roleObj;

                    AdminRoleDto dto = new AdminRoleDto();
                    dto.setId(getLongValue(roleMap.get("id")));
                    dto.setName((String) roleMap.get("name"));
                    dto.setDescription((String) roleMap.get("description"));
                    dto.setCreatedAt(parseDateTime(roleMap.get("createdAt")));

                    // Fetch role permissions
                    Set<String> permissionNames = fetchRolePermissionNames(dto.getId());
                    dto.setPermissionNames(permissionNames);

                    roles.add(dto);
                }
                return ResponseEntity.ok(roles);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error fetching roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminRoleDto> getRoleById(@PathVariable Long id) {
        try {
            logger.info("Fetching role {} from IAM", id);
            ResponseEntity<Object> response = restTemplate.getForEntity(
                iamUrl + "/roles/" + id, Object.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> roleMap = (Map<String, Object>) response.getBody();

                AdminRoleDto dto = new AdminRoleDto();
                dto.setId(getLongValue(roleMap.get("id")));
                dto.setName((String) roleMap.get("name"));
                dto.setDescription((String) roleMap.get("description"));
                dto.setCreatedAt(parseDateTime(roleMap.get("createdAt")));

                Set<String> permissionNames = fetchRolePermissionNames(dto.getId());
                dto.setPermissionNames(permissionNames);

                return ResponseEntity.ok(dto);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new role
     */
    @PostMapping
    public ResponseEntity<AdminRoleDto> createRole(@RequestBody AdminRoleDto dto) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("name", dto.getName());
            request.put("description", dto.getDescription());

            ResponseEntity<Object> response = restTemplate.postForEntity(
                iamUrl + "/roles", request, Object.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error creating role: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update role
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminRoleDto> updateRole(@PathVariable Long id, @RequestBody AdminRoleDto dto) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("name", dto.getName());
            request.put("description", dto.getDescription());

            restTemplate.put(iamUrl + "/roles/" + id, request);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error updating role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete role
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        try {
            restTemplate.delete(iamUrl + "/roles/" + id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign permission to role
     */
    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            restTemplate.postForEntity(iamUrl + "/roles/" + roleId + "/permissions/" + permissionId, null, Object.class);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error assigning permission {} to role {}: {}", permissionId, roleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove permission from role
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermissionFromRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            restTemplate.delete(iamUrl + "/roles/" + roleId + "/permissions/" + permissionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing permission {} from role {}: {}", permissionId, roleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods
    private Set<String> fetchRolePermissionNames(Long roleId) {
        try {
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                iamUrl + "/roles/" + roleId + "/permissions", Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Set<String> permissionNames = new HashSet<>();
                for (Object permObj : response.getBody()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> permMap = (Map<String, Object>) permObj;
                    permissionNames.add((String) permMap.get("name"));
                }
                return permissionNames;
            }
        } catch (Exception e) {
            logger.error("Error fetching permissions for role {}: {}", roleId, e.getMessage());
        }
        return new HashSet<>();
    }

    private Long getLongValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }

    private OffsetDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String text) {
                return OffsetDateTime.parse(text);
            }
        } catch (Exception e) {
            logger.debug("Error parsing date: {}", e.getMessage());
        }
        return null;
    }
}

