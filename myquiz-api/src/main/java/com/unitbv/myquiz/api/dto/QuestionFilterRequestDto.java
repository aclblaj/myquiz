package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canonical request DTO for question filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestionFilterRequestDto extends BasePaginationDto {
    private String course;
    private Long courseId;
    private Long authorId;
    private Long questionBank;
    private QuestionType questionType;
}
