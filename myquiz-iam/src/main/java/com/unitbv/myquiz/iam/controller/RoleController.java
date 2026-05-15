package com.unitbv.myquiz.iam.controller;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.Role;
import com.unitbv.myquiz.iam.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST Controller for Role management.
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Get all roles
     * @return List of all roles
     */
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Get role by ID
     * @param id Role ID
     * @return Role if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new role
     * @param role Role data (name and description)
     * @return Created role
     */
    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        try {
            Role created = roleService.createRole(role.getName(), role.getDescription());
            return ResponseEntity.status(201).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing role
     * @param id Role ID
     * @param role Updated role data
     * @return Updated role
     */
    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            Role updated = roleService.updateRole(id, role.getName(), role.getDescription());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a role
     * @param id Role ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all permissions for a role
     * @param id Role ID
     * @return Set of permissions
     */
    @GetMapping("/{id}/permissions")
    public ResponseEntity<Set<Permission>> getRolePermissions(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roleService.getRolePermissions(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add permission to role
     * @param roleId Role ID
     * @param permissionId Permission ID
     * @return Updated role
     */
    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Role> addPermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            return ResponseEntity.ok(roleService.addPermissionToRole(roleId, permissionId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove permission from role
     * @param roleId Role ID
     * @param permissionId Permission ID
     * @return Updated role
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Role> removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            return ResponseEntity.ok(roleService.removePermissionFromRole(roleId, permissionId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

