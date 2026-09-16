package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical export payload for question bank export endpoints.
 */
@Schema(description = "Question bank export view DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankExportDto {

    private QuestionBankDto questionBank;

    private List<QuestionBankExportAuthorSectionDto> authorSections = new ArrayList<>();
}
