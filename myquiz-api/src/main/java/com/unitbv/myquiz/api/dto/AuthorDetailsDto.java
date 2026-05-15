package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Detailed author view including all question banks, questions per bank, and errors per bank.
 * Returned by {@code GET /api/authors/{id}/details}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailsDto {
    private AuthorDto author;
    private List<QuestionBankDto> questionBanks;
    private Map<Long, List<QuestionDto>> questionsByQuestionBank;
    private Map<Long, List<QuestionErrorDto>> errorsByQuestionBank;
}
