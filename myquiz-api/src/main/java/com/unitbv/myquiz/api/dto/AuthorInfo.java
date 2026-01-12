package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Helper class to hold minimal author information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorInfo {
    Long id;
    String name;
}
