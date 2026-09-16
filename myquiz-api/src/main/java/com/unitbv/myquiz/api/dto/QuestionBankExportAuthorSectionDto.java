package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical author section DTO inside a question bank export payload.
 */
@Schema(description = "Question bank export section for a single author")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankExportAuthorSectionDto {

    private AuthorDto author;

    private List<QuestionDto> multipleChoiceQuestions = new ArrayList<>();

    private List<QuestionDto> trueFalseQuestions = new ArrayList<>();

    private List<QuestionErrorDto> errors = new ArrayList<>();

    private List<QuestionDuplicateDto> duplicateQuestions = new ArrayList<>();
}
