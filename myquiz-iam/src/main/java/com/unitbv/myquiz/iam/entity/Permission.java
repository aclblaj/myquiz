package com.unitbv.myquiz.iam.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing a permission in the system.
 * Permissions define what actions can be performed on specific resources.
 */
@Entity
@Table(name = "permissions")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "resource", length = 50)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private PermissionAction action;
}

