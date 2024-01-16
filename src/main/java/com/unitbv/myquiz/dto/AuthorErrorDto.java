package com.unitbv.myquiz.dto;

import com.unitbv.myquiz.entities.AuthorError;

public class AuthorErrorDto {

    private Long id;
    private Integer row;
    private String description;
    private String authorName;

    private String source;

    public AuthorErrorDto(Long id, Integer row, String description, String authorName, String source) {
        this.id = id;
        this.row = row;
        this.description = description;
        this.authorName = authorName;
        this.source = source;
    }

    public AuthorErrorDto(AuthorError error) {
        this.id = error.getId();
        this.row = error.getRowNumber();
        this.description = error.getDescription();
        this.authorName = error.getAuthor().getName();
        this.source = error.getSource();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
