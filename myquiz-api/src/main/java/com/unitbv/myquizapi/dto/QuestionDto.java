package com.unitbv.myquizapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Question entity.
 */
@Schema(description = "Question DTO for quiz questions")
public class QuestionDto {

    @Schema(description = "Unique identifier of the question")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Question title")
    @JsonProperty("title")
    private String title;

    @Schema(description = "Question text", required = true)
    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 1000, message = "Question text cannot exceed 1000 characters")
    @JsonProperty("text")
    private String text;

    @Schema(description = "Chapter or topic")
    @JsonProperty("chapter")
    private String chapter;

    @Schema(description = "Author name")
    @JsonProperty("authorName")
    private String authorName;

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

    // Default constructor
    public QuestionDto() {}

    // Constructor with basic fields - entity conversion will be handled in service layer
    public QuestionDto(Long id, String title, String text, String chapter) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.chapter = chapter;
    }

    public QuestionDto(Long id, String title, int row, String text, String chapter, String response1, String response2, String response3, String response4, Double weightResponse1, Double weightResponse2, Double weightResponse3, Double weightResponse4, String authorName, Double weightTrue, Double weightFalse) {
        this.id = id;
        this.title = title;
        this.row = row;
        this.text = text;
        this.chapter = chapter;
        this.response1 = response1;
        this.response2 = response2;
        this.response3 = response3;
        this.response4 = response4;
        this.weightResponse1 = weightResponse1;
        this.weightResponse2 = weightResponse2;
        this.weightResponse3 = weightResponse3;
        this.weightResponse4 = weightResponse4;
        this.authorName = authorName;
        this.weightTrue = weightTrue;
        this.weightFalse = weightFalse;
    }

    // Constructor for MC questions
    public QuestionDto(Long id, String chapter, int row, String title, String text,
                       String response1, String response2, String response3, String response4,
                       Double weightResponse1, Double weightResponse2, Double weightResponse3, Double weightResponse4,
                       String authorName) {
        this.id = id;
        this.chapter = chapter;
        this.row = row;
        this.title = title;
        this.text = text;
        this.response1 = response1;
        this.response2 = response2;
        this.response3 = response3;
        this.response4 = response4;
        this.weightResponse1 = weightResponse1;
        this.weightResponse2 = weightResponse2;
        this.weightResponse3 = weightResponse3;
        this.weightResponse4 = weightResponse4;
        this.authorName = authorName;
    }

    // Constructor for TF questions
    public QuestionDto(Long id, String chapter, String title, int row, String text,
                       Double weightTrue, Double weightFalse, String response1, String authorName) {
        this.id = id;
        this.chapter = chapter;
        this.title = title;
        this.row = row;
        this.text = text;
        this.weightTrue = weightTrue;
        this.weightFalse = weightFalse;
        this.response1 = response1;
        this.authorName = authorName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getResponse1() {
        return response1;
    }

    public void setResponse1(String response1) {
        this.response1 = response1;
    }

    public String getResponse2() {
        return response2;
    }

    public void setResponse2(String response2) {
        this.response2 = response2;
    }

    public String getResponse3() {
        return response3;
    }

    public void setResponse3(String response3) {
        this.response3 = response3;
    }

    public String getResponse4() {
        return response4;
    }

    public void setResponse4(String response4) {
        this.response4 = response4;
    }

    public Double getWeightResponse1() {
        return weightResponse1;
    }

    public void setWeightResponse1(Double weightResponse1) {
        this.weightResponse1 = weightResponse1;
    }

    public Double getWeightResponse2() {
        return weightResponse2;
    }

    public void setWeightResponse2(Double weightResponse2) {
        this.weightResponse2 = weightResponse2;
    }

    public Double getWeightResponse3() {
        return weightResponse3;
    }

    public void setWeightResponse3(Double weightResponse3) {
        this.weightResponse3 = weightResponse3;
    }

    public Double getWeightResponse4() {
        return weightResponse4;
    }

    public void setWeightResponse4(Double weightResponse4) {
        this.weightResponse4 = weightResponse4;
    }

    public Double getWeightTrue() {
        return weightTrue;
    }

    public void setWeightTrue(Double weightTrue) {
        this.weightTrue = weightTrue;
    }

    public Double getWeightFalse() {
        return weightFalse;
    }

    public void setWeightFalse(Double weightFalse) {
        this.weightFalse = weightFalse;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    // Alias for text field - for backward compatibility with templates
    public String getQuestion() {
        return text;
    }

    public void setQuestion(String question) {
        this.text = question;
    }

    @Override
    public String toString() {
        return "QuestionDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
