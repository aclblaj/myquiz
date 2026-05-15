package com.unitbv.myquiz.auth.controller;

import com.unitbv.myquiz.auth.dto.AdminPermissionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST Controller for permission management in admin interface
 */
@RestController
@RequestMapping("/api/admin/permissions")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class PermissionManagementController {

    private static final Logger logger = LoggerFactory.getLogger(PermissionManagementController.class);

    private final RestTemplate restTemplate;
    private final String iamUrl;

    public PermissionManagementController(RestTemplate restTemplate,
                                         @Value("${MYQUIZ_IAM_URL}") String iamUrl) {
        this.restTemplate = restTemplate;
        this.iamUrl = iamUrl;
    }

    /**
     * Get all permissions
     */
    @GetMapping
    public ResponseEntity<List<AdminPermissionDto>> getAllPermissions() {
        try {
            logger.info("Fetching all permissions from IAM");
            ResponseEntity<Object[]> response = restTemplate.getForEntity(
                iamUrl + "/permissions", Object[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<AdminPermissionDto> permissions = new ArrayList<>();
                for (Object permObj : response.getBody()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> permMap = (Map<String, Object>) permObj;

                    AdminPermissionDto dto = new AdminPermissionDto();
                    dto.setId(getLongValue(permMap.get("id")));
                    dto.setName((String) permMap.get("name"));
                    dto.setDescription((String) permMap.get("description"));
                    dto.setResource((String) permMap.get("resource"));
                    dto.setAction((String) permMap.get("action"));

                    permissions.add(dto);
                }
                return ResponseEntity.ok(permissions);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error fetching permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get permission by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminPermissionDto> getPermissionById(@PathVariable Long id) {
        try {
            logger.info("Fetching permission {} from IAM", id);
            ResponseEntity<Object> response = restTemplate.getForEntity(
                iamUrl + "/permissions/" + id, Object.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> permMap = (Map<String, Object>) response.getBody();

                AdminPermissionDto dto = new AdminPermissionDto();
                dto.setId(getLongValue(permMap.get("id")));
                dto.setName((String) permMap.get("name"));
                dto.setDescription((String) permMap.get("description"));
                dto.setResource((String) permMap.get("resource"));
                dto.setAction((String) permMap.get("action"));

                return ResponseEntity.ok(dto);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching permission {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }
}

