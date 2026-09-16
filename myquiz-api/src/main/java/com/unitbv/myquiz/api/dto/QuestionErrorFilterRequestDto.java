package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Canonical request DTO for question error filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestionErrorFilterRequestDto extends PaginationRequestDto {
    @Size(max = 200, message = "Course cannot exceed 200 characters")
    @JsonAlias("selectedCourse")
    private String course;
    @Positive(message = "Course ID must be positive")
    @JsonAlias("selectedCourseId")
    private Long courseId;
    @Size(max = 200, message = "Author cannot exceed 200 characters")
    @JsonAlias("selectedAuthor")
    private String author;
    @Positive(message = "Question bank ID must be positive")
    @JsonAlias("selectedQuestionBankId")
    private Long questionBankId;
}
