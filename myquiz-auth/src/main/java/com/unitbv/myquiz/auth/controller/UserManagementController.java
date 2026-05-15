package com.unitbv.myquiz.auth.controller;

import com.unitbv.myquiz.auth.dto.AdminUserDto;
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
 * REST Controller for user management in admin interface
 */
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    private final RestTemplate restTemplate;
    private final String iamUrl;

    public UserManagementController(RestTemplate restTemplate,
                                   @Value("${MYQUIZ_IAM_URL}") String iamUrl) {
        this.restTemplate = restTemplate;
        this.iamUrl = iamUrl;
    }

    /**
     * Get all users with their roles
     */
    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        try {
            logger.info("Fetching all users from IAM");
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                iamUrl + "/users", Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<AdminUserDto> users = new ArrayList<>();
                for (Object userObj : response.getBody()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userObj;

                    AdminUserDto dto = new AdminUserDto();
                    dto.setId(getLongValue(userMap.get("id")));
                    dto.setUsername((String) userMap.get("username"));
                    dto.setEmail((String) userMap.get("email"));
                    dto.setEnabled((Boolean) userMap.get("enabled"));
                    dto.setCreatedAt(parseDateTime(userMap.get("createdAt")));
                    dto.setUpdatedAt(parseDateTime(userMap.get("updatedAt")));

                    // Fetch user roles
                    Set<String> roleNames = fetchUserRoleNames(dto.getId());
                    dto.setRoleNames(roleNames);

                    users.add(dto);
                }
                return ResponseEntity.ok(users);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        try {
            logger.info("Fetching user {} from IAM", id);
            ResponseEntity<Object> response = restTemplate.getForEntity(
                iamUrl + "/users/" + id, Object.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) response.getBody();

                AdminUserDto dto = new AdminUserDto();
                dto.setId(getLongValue(userMap.get("id")));
                dto.setUsername((String) userMap.get("username"));
                dto.setEmail((String) userMap.get("email"));
                dto.setEnabled((Boolean) userMap.get("enabled"));
                dto.setCreatedAt(parseDateTime(userMap.get("createdAt")));
                dto.setUpdatedAt(parseDateTime(userMap.get("updatedAt")));

                Set<String> roleNames = fetchUserRoleNames(dto.getId());
                dto.setRoleNames(roleNames);

                return ResponseEntity.ok(dto);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update user enabled status
     */
    @PutMapping("/{id}/enabled")
    public ResponseEntity<Void> updateUserEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            String url = iamUrl + "/users/" + id + (enabled ? "/enable" : "/disable");
            restTemplate.put(url, null);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error updating user {} enabled status: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            restTemplate.postForEntity(iamUrl + "/users/" + userId + "/roles/" + roleId, null, Object.class);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error assigning role {} to user {}: {}", roleId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove role from user
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            restTemplate.delete(iamUrl + "/users/" + userId + "/roles/" + roleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing role {} from user {}: {}", roleId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            restTemplate.delete(iamUrl + "/users/" + id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods
    private Set<String> fetchUserRoleNames(Long userId) {
        try {
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                iamUrl + "/users/" + userId + "/roles", Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Set<String> roleNames = new HashSet<>();
                for (Object roleObj : response.getBody()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> roleMap = (Map<String, Object>) roleObj;
                    roleNames.add((String) roleMap.get("name"));
                }
                return roleNames;
            }
        } catch (Exception e) {
            logger.error("Error fetching roles for user {}: {}", userId, e.getMessage());
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

