package com.unitbv.myquiz.iam.service.impl;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.PermissionAction;
import com.unitbv.myquiz.iam.repository.PermissionRepository;
import com.unitbv.myquiz.iam.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of PermissionService.
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Permission createPermission(String name, String description, String resource, PermissionAction action) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setResource(resource);
        permission.setAction(action);
        return permissionRepository.save(permission);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> getPermissionById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    public Optional<Permission> getPermissionByName(String name) {
        return permissionRepository.findByName(name);
    }

    @Override
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    @Override
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }
}

