package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Data Transfer Object for Question Correction operations.
 * Contains both original and modified question data for AI-powered corrections.
 */
@Schema(description = "Question correction DTO containing original and modified question data")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"correctionType", "modelUsed", "language"})
public class QuestionCorrectionDto {

    @Schema(description = "Original question data")
    @JsonProperty("originalQuestion")
    @NotNull(message = "Original question cannot be null")
    @Valid
    private QuestionDto originalQuestion = new QuestionDto();

    @Schema(description = "Modified/corrected question data")
    @JsonProperty("modifiedQuestion")
    private QuestionDto modifiedQuestion = new QuestionDto();

    @Schema(description = "Type of correction applied (grammar, improve, alternatives, explain)")
    @JsonProperty("correctionType")
    private String correctionType;

    @Schema(description = "AI model used for correction")
    @JsonProperty("modelUsed")
    @JsonAlias("model")
    private String modelUsed;

    @Schema(description = "Additional notes or explanation from AI")
    @JsonProperty("correctionNotes")
    private String correctionNotes;

    @Schema(description = "Language for correction (ro/en)")
    @JsonProperty("language")
    private String language;

    public QuestionCorrectionDto(QuestionDto originalQuestion) {
        this.originalQuestion = originalQuestion;
        this.modifiedQuestion = new QuestionDto();
    }

    public QuestionCorrectionDto(QuestionDto originalQuestion, QuestionDto modifiedQuestion) {
        this.originalQuestion = originalQuestion;
        this.modifiedQuestion = modifiedQuestion;
    }

    /**
     * Copies all content fields from {@code originalQuestion} into a fresh {@code modifiedQuestion},
     * excluding output-only fields (errors, duplicates, duplicateCount).
     */
    public void copyOriginalToModified() {
        if (this.originalQuestion == null) {
            return;
        }
        QuestionDto src = this.originalQuestion;
        QuestionDto copy = new QuestionDto();
        copy.setId(src.getId());
        copy.setTitle(src.getTitle());
        copy.setText(src.getText());
        copy.setAnswerReferenceText(src.getAnswerReferenceText());
        copy.setChapter(src.getChapter());
        copy.setAuthorName(src.getAuthorName());
        copy.setCourse(src.getCourse());
        copy.setQuestionBankName(src.getQuestionBankName());
        copy.setType(src.getType());
        copy.setResponse1(src.getResponse1());
        copy.setResponse2(src.getResponse2());
        copy.setResponse3(src.getResponse3());
        copy.setResponse4(src.getResponse4());
        copy.setWeightResponse1(src.getWeightResponse1());
        copy.setWeightResponse2(src.getWeightResponse2());
        copy.setWeightResponse3(src.getWeightResponse3());
        copy.setWeightResponse4(src.getWeightResponse4());
        copy.setWeightTrue(src.getWeightTrue());
        copy.setWeightFalse(src.getWeightFalse());
        this.modifiedQuestion = copy;
    }
}
