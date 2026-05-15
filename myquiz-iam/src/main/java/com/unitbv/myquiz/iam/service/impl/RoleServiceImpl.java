package com.unitbv.myquiz.iam.service.impl;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.Role;
import com.unitbv.myquiz.iam.repository.PermissionRepository;
import com.unitbv.myquiz.iam.repository.RoleRepository;
import com.unitbv.myquiz.iam.service.RoleService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of RoleService.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Role createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setCreatedAt(OffsetDateTime.now());
        return roleRepository.save(role);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public Role updateRole(Long id, String name, String description) {
        Optional<Role> optionalRole = roleRepository.findById(id);
        if (optionalRole.isPresent()) {
            Role role = optionalRole.get();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        }
        throw new RuntimeException("Role not found with id: " + id);
    }

    @Override
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    public Role addPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));

        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    @Override
    public Role removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));

        role.getPermissions().remove(permission);
        return roleRepository.save(role);
    }

    @Override
    public Set<Permission> getRolePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        return role.getPermissions();
    }
}

