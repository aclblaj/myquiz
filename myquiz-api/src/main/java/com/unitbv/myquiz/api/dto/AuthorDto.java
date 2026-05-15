package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object for Author entity.
 * <p>
 * Used as the canonical output type for all author endpoints.
 * For write operations (create / update) prefer {@link AuthorUpsertDto}.
 */
@Schema(description = "Author DTO for questionBank authors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "name", "initials", "numberOfQuestions"})
public class AuthorDto {

    @Schema(description = "Unique identifier of the author")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Author's full name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Author's initials")
    @JsonProperty("initials")
    private String initials;

    @Schema(description = "Number of multiple choice questions")
    @JsonProperty("numberOfMultipleChoiceQuestions")
    @Builder.Default
    private Long numberOfMultipleChoiceQuestions = 0L;

    @Schema(description = "Number of true/false questions")
    @JsonProperty("numberOfTrueFalseQuestions")
    @Builder.Default
    private Long numberOfTrueFalseQuestions = 0L;

    @Schema(description = "Number of questions with errors")
    @JsonProperty("numberOfErrors")
    @Builder.Default
    private Long numberOfErrors = 0L;

    @Schema(description = "Total number of questions")
    @JsonProperty("numberOfQuestions")
    @Builder.Default
    private Long numberOfQuestions = 0L;

    @Schema(description = "Number of questions with duplicates")
    @JsonProperty("numberOfDuplicates")
    @Builder.Default
    private Long numberOfDuplicates = 0L;

    @Schema(description = "questionBank name")
    @JsonProperty("questionBankName")
    private String questionBankName;

    @Schema(description = "Template type used")
    @JsonProperty("templateType")
    private String templateType;

    @Schema(description = "Course name")
    @JsonProperty("course")
    private String course;
}
