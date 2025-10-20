package com.unitbv.myquizapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Author entity.
 */
@Schema(description = "Author DTO for quiz authors")
public class AuthorDto {

    @Schema(description = "Unique identifier of the author")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Author's full name", required = true)
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Author's initials")
    @JsonProperty("initials")
    private String initials;

    @Schema(description = "Number of multiple choice questions")
    @JsonProperty("numberOfMultipleChoiceQuestions")
    private Long numberOfMultipleChoiceQuestions = 0L;

    @Schema(description = "Number of true/false questions")
    @JsonProperty("numberOfTrueFalseQuestions")
    private Long numberOfTrueFalseQuestions = 0L;

    @Schema(description = "Number of questions with errors")
    @JsonProperty("numberOfErrors")
    private Long numberOfErrors = 0L;

    @Schema(description = "Total number of questions")
    @JsonProperty("numberOfQuestions")
    private Long numberOfQuestions = 0L;

    @Schema(description = "Quiz name")
    @JsonProperty("quizName")
    private String quizName;

    @Schema(description = "Template type used")
    @JsonProperty("templateType")
    private String templateType;

    @Schema(description = "Course name")
    @JsonProperty("course")
    private String course;

    // Default constructor
    public AuthorDto() {}

    // Constructor with basic fields - entity conversion will be handled in service layer
    public AuthorDto(Long id, String name, String initials) {
        this.id = id;
        this.name = name;
        this.initials = initials;
        this.numberOfMultipleChoiceQuestions = 0L;
        this.numberOfTrueFalseQuestions = 0L;
        this.numberOfErrors = 0L;
        this.numberOfQuestions = 0L;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public Long getNumberOfMultipleChoiceQuestions() {
        return numberOfMultipleChoiceQuestions;
    }

    public void setNumberOfMultipleChoiceQuestions(Long numberOfMultipleChoiceQuestions) {
        this.numberOfMultipleChoiceQuestions = numberOfMultipleChoiceQuestions;
    }

    public Long getNumberOfTrueFalseQuestions() {
        return numberOfTrueFalseQuestions;
    }

    public void setNumberOfTrueFalseQuestions(Long numberOfTrueFalseQuestions) {
        this.numberOfTrueFalseQuestions = numberOfTrueFalseQuestions;
    }

    public Long getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(Long numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }

    public Long getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Long numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public String getQuizName() {
        return quizName;
    }

    public void setQuizName(String quizName) {
        this.quizName = quizName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "AuthorDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", initials='" + initials + '\'' +
                ", numberOfQuestions=" + numberOfQuestions +
                '}';
    }
}
