package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Canonical response DTO for question bank filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class QuestionBankFilterResponseDto extends BaseFilterDto {
    private List<QuestionBankDto> questionBanks;
    private List<CourseDto> courses;
}
