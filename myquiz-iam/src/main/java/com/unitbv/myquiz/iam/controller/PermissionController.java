package com.unitbv.myquiz.iam.controller;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Permission management.
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Get all permissions
     * @return List of all permissions
     */
    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    /**
     * Get permission by ID
     * @param id Permission ID
     * @return Permission if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get permissions by resource
     * @param resource Resource name
     * @return List of permissions for the resource
     */
    @GetMapping("/resource/{resource}")
    public ResponseEntity<List<Permission>> getPermissionsByResource(@PathVariable String resource) {
        return ResponseEntity.ok(permissionService.getPermissionsByResource(resource));
    }
}

