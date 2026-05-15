package com.unitbv.myquiz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Canonical admin-facing permission DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPermissionDto {
	private Long id;
	private String name;
	private String description;
	private String resource;
	private String action;
}

