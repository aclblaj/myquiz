package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("author")
    private AuthorDto author;

    @JsonProperty("multipleChoiceQuestions")
    private List<QuestionDto> multipleChoiceQuestions = new ArrayList<>();

    @JsonProperty("trueFalseQuestions")
    private List<QuestionDto> trueFalseQuestions = new ArrayList<>();

    @JsonProperty("errors")
    private List<QuestionErrorDto> errors = new ArrayList<>();

    @JsonProperty("duplicateQuestions")
    private List<QuestionDuplicateDto> duplicateQuestions = new ArrayList<>();
}
