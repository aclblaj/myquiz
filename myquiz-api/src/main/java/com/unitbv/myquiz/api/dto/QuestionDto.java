package com.unitbv.myquiz.api.dto;

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
    private Long id;

    @Schema(description = "Question title")
    private String title;

    @Schema(description = "Question text", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 1000, message = "Question text cannot exceed 1000 characters")
    private String text;

    @Schema(description = "Optional answer reference text", maxLength = 2000)
    @Size(max = 2000, message = "Answer reference cannot exceed 2000 characters")
    private String answerReferenceText;

    @Schema(description = "Chapter or topic")
    private String chapter;

    @Schema(description = "Author info")
    private AuthorInfo author;

    @Schema(description = "QuestionBank name")
    private String questionBankName;

    @Schema(description = "QuestionBank ID")
    private Long questionBankId;

    @Schema(description = "Response 1")
    private String response1;

    @Schema(description = "Response 2")
    private String response2;

    @Schema(description = "Response 3")
    private String response3;

    @Schema(description = "Response 4")
    private String response4;

    @Schema(description = "Weight for response 1")
    private Double weightResponse1;

    @Schema(description = "Weight for response 2")
    private Double weightResponse2;

    @Schema(description = "Weight for response 3")
    private Double weightResponse3;

    @Schema(description = "Weight for response 4")
    private Double weightResponse4;

    @Schema(description = "Weight for true answer")
    private Double weightTrue;

    @Schema(description = "Weight for false answer")
    private Double weightFalse;

    @Schema(description = "Row number")
    private Integer row;

    @Schema(description = "Course name")
    private String course;

    @Schema(description = "Question type")
    private QuestionType type;

    @Schema(description = "Validation errors for this question")
    private List<QuestionErrorDto> errors = new ArrayList<>();

    @Schema(description = "Number of duplicate questions linked to this question")
    private Integer duplicateCount = 0;

    @Schema(description = "Duplicate questions linked to this question")
    private List<QuestionDuplicateDto> duplicates = new ArrayList<>();
}
