package com.unitbv.myquiz.dto;

import com.unitbv.myquiz.entities.Author;

public class AuthorDto {

    private Long id;
    private String name;
    private String initials;

    private Integer numberOfQuestions;
    private Integer numberOfErrors;
    public AuthorDto(Author author) {
        this.id = author.getId();
        this.name = author.getName();
        this.initials = author.getInitials();
        this.numberOfQuestions = author.getQuestions().size();
        this.numberOfErrors = author.getAuthorErrors().size();
    }

    public AuthorDto(Long id, String name, String initials, Integer numberOfQuestions, Integer numberOfErrors) {
        this.id = id;
        this.name = name;
        this.initials = initials;
        this.numberOfQuestions = numberOfQuestions;
        this.numberOfErrors = numberOfErrors;
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

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(Integer numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }
}
