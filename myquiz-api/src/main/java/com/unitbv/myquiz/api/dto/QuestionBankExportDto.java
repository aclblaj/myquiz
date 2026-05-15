package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("questionBank")
    private QuestionBankDto questionBank;

    @JsonProperty("authorSections")
    private List<QuestionBankExportAuthorSectionDto> authorSections = new ArrayList<>();
}
