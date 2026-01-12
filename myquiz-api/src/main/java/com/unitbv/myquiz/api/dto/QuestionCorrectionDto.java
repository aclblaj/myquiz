package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for Question Correction operations.
 * Contains both original and modified question data for AI-powered corrections.
 * Uses QuestionDto for both original and modified questions to avoid duplication.
 */
@Schema(description = "Question correction DTO containing original and modified question data")
public class QuestionCorrectionDto {

    @Schema(description = "Original question data")
    @JsonProperty("originalQuestion")
    @NotNull(message = "Original question cannot be null")
    @Valid
    private QuestionDto originalQuestion;

    @Schema(description = "Modified/corrected question data")
    @JsonProperty("modifiedQuestion")
    private QuestionDto modifiedQuestion;

    // Correction Metadata
    @Schema(description = "Type of correction applied (grammar, improve, alternatives, explain)")
    @JsonProperty("correctionType")
    private String correctionType;

    @Schema(description = "AI model used for correction")
    @JsonProperty("modelUsed")
    private String modelUsed;

    @Schema(description = "Additional notes or explanation from AI")
    @JsonProperty("correctionNotes")
    private String correctionNotes;

    @Schema(description = "Language for correction (ro/en)")
    @JsonProperty("language")
    private String language;

    // Constructors
    public QuestionCorrectionDto() {
        this.originalQuestion = new QuestionDto();
        this.modifiedQuestion = new QuestionDto();
    }

    public QuestionCorrectionDto(QuestionDto originalQuestion) {
        this.originalQuestion = originalQuestion;
        this.modifiedQuestion = new QuestionDto();
    }

    public QuestionCorrectionDto(QuestionDto originalQuestion, QuestionDto modifiedQuestion) {
        this.originalQuestion = originalQuestion;
        this.modifiedQuestion = modifiedQuestion;
    }

    // Getters and Setters
    public QuestionDto getOriginalQuestion() {
        return originalQuestion;
    }

    public void setOriginalQuestion(QuestionDto originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    public QuestionDto getModifiedQuestion() {
        return modifiedQuestion;
    }

    public void setModifiedQuestion(QuestionDto modifiedQuestion) {
        this.modifiedQuestion = modifiedQuestion;
    }

    public String getCorrectionType() {
        return correctionType;
    }

    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getCorrectionNotes() {
        return correctionNotes;
    }

    public void setCorrectionNotes(String correctionNotes) {
        this.correctionNotes = correctionNotes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    // Helper methods
    /**
     * Copy original question to modified question
     */
    public void copyOriginalToModified() {
        if (this.originalQuestion != null) {
            QuestionDto copy = new QuestionDto();
            copy.setId(originalQuestion.getId());
            copy.setTitle(originalQuestion.getTitle());
            copy.setText(originalQuestion.getText());
            copy.setChapter(originalQuestion.getChapter());
            copy.setAuthorName(originalQuestion.getAuthorName());
            copy.setCourse(originalQuestion.getCourse());
            copy.setQuizName(originalQuestion.getQuizName());
            copy.setType(originalQuestion.getType());
            copy.setResponse1(originalQuestion.getResponse1());
            copy.setResponse2(originalQuestion.getResponse2());
            copy.setResponse3(originalQuestion.getResponse3());
            copy.setResponse4(originalQuestion.getResponse4());
            copy.setWeightResponse1(originalQuestion.getWeightResponse1());
            copy.setWeightResponse2(originalQuestion.getWeightResponse2());
            copy.setWeightResponse3(originalQuestion.getWeightResponse3());
            copy.setWeightResponse4(originalQuestion.getWeightResponse4());
            copy.setWeightTrue(originalQuestion.getWeightTrue());
            copy.setWeightFalse(originalQuestion.getWeightFalse());
            this.modifiedQuestion = copy;
        }
    }

    @Override
    public String toString() {
        return "QuestionCorrectionDto{" +
                "originalQuestion=" + (originalQuestion != null ? originalQuestion.getId() : null) +
                ", modifiedQuestion=" + (modifiedQuestion != null ? modifiedQuestion.getId() : null) +
                ", correctionType='" + correctionType + '\'' +
                ", modelUsed='" + modelUsed + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}

