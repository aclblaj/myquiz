package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canonical request DTO for author filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorFilterRequestDto extends BasePaginationDto {
    private String course;
    private Long courseId;
    private Long authorId;
    private Long questionBankId;
}
