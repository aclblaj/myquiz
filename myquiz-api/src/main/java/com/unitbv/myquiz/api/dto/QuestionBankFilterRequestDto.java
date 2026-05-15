package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Canonical request DTO for question bank filtering operations.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class QuestionBankFilterRequestDto extends BasePaginationDto {
    private Long courseId;

    public QuestionBankFilterRequestDto() {
        setPage(1);
        setPageSize(10);
    }
}

