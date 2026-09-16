package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Canonical request DTO for author filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorFilterRequestDto extends PaginationRequestDto {
    @Size(max = 200, message = "Course cannot exceed 200 characters")
    private String course;
    @Positive(message = "Course ID must be positive")
    private Long courseId;
    @Positive(message = "Author ID must be positive")
    private Long authorId;
    @Positive(message = "Question bank ID must be positive")
    private Long questionBankId;
}
