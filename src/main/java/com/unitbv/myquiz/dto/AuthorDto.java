package com.unitbv.myquiz.dto;

import com.unitbv.myquiz.entities.Author;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
@lombok.ToString
public class AuthorDto {

    private Long id;
    private String name;
    private String initials;
    private Long numberOfQuestions = 0L;
    private Long numberOfMultipleChoiceQuestions = 0L;
    private Long numberOfTrueFalseQuestions = 0L;
    private Long numberOfErrors = 0L;
    public AuthorDto(Author author) {
        this.id = author.getId();
        this.name = author.getName();
        this.initials = author.getInitials();
    }

    public AuthorDto(Long id, String name, String initials) {
        this.id = id;
        this.name = name;
        this.initials = initials;
    }

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

    public Long getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Long numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Long getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(Long numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
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

}
