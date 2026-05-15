package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canonical request DTO for question error filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestionErrorFilterRequestDto extends BasePaginationDto {
    private String selectedCourse;
    private Long selectedCourseId;
    private String selectedAuthor;
    private Long selectedQuestionBankId;
}
