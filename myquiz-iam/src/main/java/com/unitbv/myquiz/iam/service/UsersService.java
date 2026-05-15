package com.unitbv.myquiz.iam.service;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.Role;
import com.unitbv.myquiz.iam.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsersService {
    
    User createUser(String username, String email, String password);

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    Optional<User> getByUsernameOrEmail(String identifier);

    Optional<User> getUserByUsername(String username);

    User updateUser(Long id, String username, String email, String password);

    void deleteUser(Long id);

    /**
     * Assign role to user
     * @param userId User ID
     * @param roleId Role ID
     * @return Updated user
     */
    User assignRoleToUser(Long userId, Long roleId);

    /**
     * Remove role from user
     * @param userId User ID
     * @param roleId Role ID
     * @return Updated user
     */
    User removeRoleFromUser(Long userId, Long roleId);

    /**
     * Get all roles for a user
     * @param userId User ID
     * @return Set of roles
     */
    Set<Role> getUserRoles(Long userId);

    /**
     * Get all permissions for a user (merged from all roles)
     * @param userId User ID
     * @return Set of permissions
     */
    Set<Permission> getUserPermissions(Long userId);

    /**
     * Check if user has a specific permission
     * @param userId User ID
     * @param permissionName Permission name
     * @return true if user has permission, false otherwise
     */
    boolean hasPermission(Long userId, String permissionName);

    /**
     * Enable user account
     * @param userId User ID
     * @return Updated user
     */
    User enableUser(Long userId);

    /**
     * Disable user account
     * @param userId User ID
     * @return Updated user
     */
    User disableUser(Long userId);
}

