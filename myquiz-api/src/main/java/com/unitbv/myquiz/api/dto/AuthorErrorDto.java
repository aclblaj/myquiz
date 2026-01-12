package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for Author error responses.
 */
@Schema(description = "Author error DTO for error handling")
public class AuthorErrorDto {

    @Schema(description = "Error ID")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Error code")
    @JsonProperty("errorCode")
    private String errorCode;

    @Schema(description = "Error message")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Error description")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Author ID that caused the error")
    @JsonProperty("authorId")
    private Long authorId;

    @Schema(description = "Timestamp when error occurred")
    @JsonProperty("timestamp")
    private java.time.LocalDateTime timestamp;

    @Schema(description = "Author name")
    @JsonProperty("authorName")
    private String authorName;

    @Schema(description = "Row number where error occurred")
    @JsonProperty("row")
    private Integer row;

    @Schema(description = "Quiz name")
    @JsonProperty("quizName")
    private String quizName;

    @Schema(description = "Quiz id")
    @JsonProperty("quizId")
    private Long quizId;

    @Schema(description = "Date created")
    @JsonProperty("dateCreated")
    private java.util.Date dateCreated;

    @Schema(description = "Error status")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Question ID associated with this error")
    @JsonProperty("questionId")
    private Long questionId;

    // Default constructor
    public AuthorErrorDto() {
        this.timestamp = java.time.LocalDateTime.now();
        this.dateCreated = new java.util.Date();
        this.status = "OPEN";
    }

    public AuthorErrorDto(String errorCode, String message, Long authorId) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.authorId = authorId;
    }

    // Constructor for creating error from details - entity conversion handled in service layer
    public AuthorErrorDto(String message, Integer row, String authorName, Long authorId) {
        this();
        this.message = message;
        this.description = message;
        this.row = row;
        this.authorName = authorName;
        this.authorId = authorId;
        this.errorCode = "QUIZ_ERROR";
    }

    // Constructor with ID
    public AuthorErrorDto(Long id, String description, Integer row, String authorName, Long authorId) {
        this();
        this.id = id;
        this.description = description;
        this.message = description;
        this.row = row;
        this.authorName = authorName;
        this.authorId = authorId;
        this.errorCode = "QUIZ_ERROR";
    }

    // Getters and setters
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuizName() {
        return quizName;
    }

    public void setQuizName(String quizName) {
        this.quizName = quizName;
    }

    public java.util.Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(java.util.Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    @Override
    public String toString() {
        return "AuthorErrorDto{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                ", authorId=" + authorId +
                ", timestamp=" + timestamp +
                '}';
    }
}
