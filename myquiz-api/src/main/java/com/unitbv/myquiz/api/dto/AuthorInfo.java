package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Helper class to hold minimal author information (id + name + initials) for dropdowns and filter lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorInfo {
    private Long id;
    private String name;
    private String initials;
}
