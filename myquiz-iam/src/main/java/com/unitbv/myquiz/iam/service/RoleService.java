package com.unitbv.myquiz.iam.service;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for Role operations.
 */
public interface RoleService {

    /**
     * Create a new role
     * @param name Role name
     * @param description Role description
     * @return Created role
     */
    Role createRole(String name, String description);

    /**
     * Get all roles
     * @return List of all roles
     */
    List<Role> getAllRoles();

    /**
     * Get role by ID
     * @param id Role ID
     * @return Optional containing the role if found
     */
    Optional<Role> getRoleById(Long id);

    /**
     * Get role by name
     * @param name Role name
     * @return Optional containing the role if found
     */
    Optional<Role> getRoleByName(String name);

    /**
     * Update role
     * @param id Role ID
     * @param name New role name
     * @param description New role description
     * @return Updated role
     */
    Role updateRole(Long id, String name, String description);

    /**
     * Delete role by ID
     * @param id Role ID
     */
    void deleteRole(Long id);

    /**
     * Add permission to role
     * @param roleId Role ID
     * @param permissionId Permission ID
     * @return Updated role
     */
    Role addPermissionToRole(Long roleId, Long permissionId);

    /**
     * Remove permission from role
     * @param roleId Role ID
     * @param permissionId Permission ID
     * @return Updated role
     */
    Role removePermissionFromRole(Long roleId, Long permissionId);

    /**
     * Get all permissions for a role
     * @param roleId Role ID
     * @return Set of permissions
     */
    Set<Permission> getRolePermissions(Long roleId);
}

