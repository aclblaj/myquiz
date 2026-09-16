package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.Positive;

/**
 * Canonical request DTO for question bank filtering operations.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class QuestionBankFilterRequestDto extends PaginationRequestDto {
    @Positive(message = "Course ID must be positive")
    private Long courseId;

}
