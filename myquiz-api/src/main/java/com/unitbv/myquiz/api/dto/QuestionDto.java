package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unitbv.myquiz.api.types.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Question entity.
 */
@Schema(description = "Question DTO for questionBank questions")
@Data
@ToString(of = {"id", "title", "text"})
public class QuestionDto {

    @Schema(description = "Unique identifier of the question")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Question title")
    @JsonProperty("title")
    private String title;

    @Schema(description = "Question text", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 1000, message = "Question text cannot exceed 1000 characters")
    @JsonProperty("text")
    private String text;

    @Schema(description = "Optional answer reference text", maxLength = 2000)
    @Size(max = 2000, message = "Answer reference cannot exceed 2000 characters")
    @JsonProperty("answerReferenceText")
    private String answerReferenceText;

    @Schema(description = "Chapter or topic")
    @JsonProperty("chapter")
    private String chapter;

    @Schema(description = "Author name")
    @JsonProperty("authorName")
    private String authorName;

    @Schema(description = "QuestionBank name")
    @JsonProperty("questionBankName")
    private String questionBankName;

    @Schema(description = "QuestionBank ID")
    @JsonProperty("questionBankId")
    private Long questionBankId;

    @Schema(description = "Response 1")
    @JsonProperty("response1")
    private String response1;

    @Schema(description = "Response 2")
    @JsonProperty("response2")
    private String response2;

    @Schema(description = "Response 3")
    @JsonProperty("response3")
    private String response3;

    @Schema(description = "Response 4")
    @JsonProperty("response4")
    private String response4;

    @Schema(description = "Weight for response 1")
    @JsonProperty("weightResponse1")
    private Double weightResponse1;

    @Schema(description = "Weight for response 2")
    @JsonProperty("weightResponse2")
    private Double weightResponse2;

    @Schema(description = "Weight for response 3")
    @JsonProperty("weightResponse3")
    private Double weightResponse3;

    @Schema(description = "Weight for response 4")
    @JsonProperty("weightResponse4")
    private Double weightResponse4;

    @Schema(description = "Weight for true answer")
    @JsonProperty("weightTrue")
    private Double weightTrue;

    @Schema(description = "Weight for false answer")
    @JsonProperty("weightFalse")
    private Double weightFalse;

    @Schema(description = "Row number")
    @JsonProperty("row")
    private Integer row;

    @Schema(description = "Course name")
    @JsonProperty("course")
    private String course;

    @Schema(description = "Question type")
    @JsonProperty("type")
    private QuestionType type;

    @Schema(description = "Validation errors for this question")
    @JsonProperty("errors")
    private List<QuestionErrorDto> errors = new ArrayList<>();

    @Schema(description = "Number of duplicate questions linked to this question")
    @JsonProperty("duplicateCount")
    private Integer duplicateCount = 0;

    @Schema(description = "Duplicate questions linked to this question")
    @JsonProperty("duplicates")
    private List<QuestionDuplicateDto> duplicates = new ArrayList<>();
}
