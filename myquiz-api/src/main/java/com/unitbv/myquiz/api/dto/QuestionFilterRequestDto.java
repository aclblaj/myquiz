package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.unitbv.myquiz.api.types.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Canonical request DTO for question filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestionFilterRequestDto extends PaginationRequestDto {
    @Size(max = 200, message = "Course cannot exceed 200 characters")
    private String course;
    @Positive(message = "Course ID must be positive")
    private Long courseId;
    @Positive(message = "Author ID must be positive")
    private Long authorId;
    @Positive(message = "Question bank ID must be positive")
    @JsonAlias("questionBank")
    private Long questionBankId;
    private QuestionType questionType;
}
