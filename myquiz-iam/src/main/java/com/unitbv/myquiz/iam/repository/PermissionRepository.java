package com.unitbv.myquiz.iam.repository;

import com.unitbv.myquiz.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Permission entity operations.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Find permission by name
     * @param name Permission name
     * @return Optional containing the permission if found
     */
    Optional<Permission> findByName(String name);

    /**
     * Find all permissions for a specific resource
     * @param resource Resource name (e.g., "COURSE", "QUIZ")
     * @return List of permissions for the resource
     */
    List<Permission> findByResource(String resource);
}

