package com.unitbv.myquiz.api.dto;

/**
 * DTO for question validation errors to display in the editor.
 */
public class QuestionErrorDto {
    private Long errorId;
    private String description;
    private Integer rowNumber;

    public QuestionErrorDto() {
    }

    public QuestionErrorDto(Long errorId, String description, Integer rowNumber) {
        this.errorId = errorId;
        this.description = description;
        this.rowNumber = rowNumber;
    }

    public Long getErrorId() {
        return errorId;
    }

    public void setErrorId(Long errorId) {
        this.errorId = errorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
}

