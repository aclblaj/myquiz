package com.unitbv.myquiz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Canonical admin-facing user DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
	private Long id;
	private String username;
	private String email;
	private Boolean enabled;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private Set<String> roleNames;
}

