package com.unitbv.myquiz.iam.service;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.PermissionAction;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Permission operations.
 */
public interface PermissionService {

    /**
     * Create a new permission
     * @param name Permission name
     * @param description Permission description
     * @param resource Resource name
     * @param action Permission action (READ/MODIFY)
     * @return Created permission
     */
    Permission createPermission(String name, String description, String resource, PermissionAction action);

    /**
     * Get all permissions
     * @return List of all permissions
     */
    List<Permission> getAllPermissions();

    /**
     * Get permission by ID
     * @param id Permission ID
     * @return Optional containing the permission if found
     */
    Optional<Permission> getPermissionById(Long id);

    /**
     * Get permission by name
     * @param name Permission name
     * @return Optional containing the permission if found
     */
    Optional<Permission> getPermissionByName(String name);

    /**
     * Get all permissions for a specific resource
     * @param resource Resource name
     * @return List of permissions for the resource
     */
    List<Permission> getPermissionsByResource(String resource);

    /**
     * Delete permission by ID
     * @param id Permission ID
     */
    void deletePermission(Long id);
}

