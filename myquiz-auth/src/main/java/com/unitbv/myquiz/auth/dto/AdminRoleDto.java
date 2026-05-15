package com.unitbv.myquiz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Canonical admin-facing role DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleDto {
	private Long id;
	private String name;
	private String description;
	private OffsetDateTime createdAt;
	private Set<String> permissionNames;
}

