package com.unitbv.myquiz.dto;

import com.unitbv.myquiz.entities.Question;

public class QuestionDto {
    private Long id;
    private String course;
    private Integer row;
    private String title;
    private String text;
    private String response1;
    private String response2;
    private String response3;
    private String response4;
    private Double weightResponse1;
    private Double weightResponse2;
    private Double weightResponse3;
    private Double weightResponse4;
    private Double weightTrue;
    private Double weightFalse;

    private String authorName;

    public QuestionDto() {
    }

    public QuestionDto(Long id, String title, Integer row, String text,
                       String response1, String response2, String response3, String response4,
                       Double weightResponse1, Double weightResponse2, Double weightResponse3, Double weightResponse4,
                       String authorName) {
        this.id = id;
        this.title = title;
        this.row = row;
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

    public QuestionDto(Question question) {
        this.id = question.getId();
        this.course = question.getCourse();
        this.title = question.getTitle();
        this.row = question.getCrtNo();
        this.text = question.getText();
        this.response1 = question.getResponse1();
        this.response2 = question.getResponse2();
        this.response3 = question.getResponse3();
        this.response4 = question.getResponse4();
        this.weightResponse1 = question.getWeightResponse1();
        this.weightResponse2 = question.getWeightResponse2();
        this.weightResponse3 = question.getWeightResponse3();
        this.weightResponse4 = question.getWeightResponse4();
        this.weightTrue = question.getWeightTrue();
        this.weightFalse = question.getWeightFalse();
        this.authorName = question.getQuizAuthor().getAuthor().getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
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
}


