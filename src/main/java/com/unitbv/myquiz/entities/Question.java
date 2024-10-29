package com.unitbv.myquiz.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Question {
    int crtNo;
    String course;
    String title;
    @Column(length = 1024)
    String text;
    @Enumerated(EnumType.STRING)
    QuestionType type;
    Double weightResponse1;
    @Column(length = 1024)
    String response1;
    Double weightResponse2;
    @Column(length = 1024)
    String response2;
    Double weightResponse3;
    @Column(length = 1024)
    String response3;
    Double weightResponse4;
    @Column(length = 1024)
    String response4;

    Double weightTrue;
    Double weightFalse;

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "quiz_author_id")
    QuizAuthor quizAuthor;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_gen")
    @SequenceGenerator(name = "question_gen", sequenceName = "question_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    public QuizAuthor getQuizAuthor() {
        return quizAuthor;
    }

    public void setQuizAuthor(QuizAuthor quizAuthor) {
        this.quizAuthor = quizAuthor;
    }

    public Question() {
    }

    public int getCrtNo() {
        return crtNo;
    }

    public void setCrtNo(int crtNo) {
        this.crtNo = crtNo;
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

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Double getWeightResponse1() {
        return weightResponse1;
    }

    public void setWeightResponse1(Double weightResponse1) {
        this.weightResponse1 = weightResponse1;
    }

    public String getResponse1() {
        return response1;
    }

    public void setResponse1(String response1) {
        this.response1 = response1;
    }

    public Double getWeightResponse2() {
        return weightResponse2;
    }

    public void setWeightResponse2(Double weightResponse2) {
        this.weightResponse2 = weightResponse2;
    }

    public String getResponse2() {
        return response2;
    }

    public void setResponse2(String response2) {
        this.response2 = response2;
    }

    public Double getWeightResponse3() {
        return weightResponse3;
    }

    public void setWeightResponse3(Double weightResponse3) {
        this.weightResponse3 = weightResponse3;
    }

    public String getResponse3() {
        return response3;
    }

    public void setResponse3(String response3) {
        this.response3 = response3;
    }

    public Double getWeightResponse4() {
        return weightResponse4;
    }

    public void setWeightResponse4(Double weightResponse4) {
        this.weightResponse4 = weightResponse4;
    }

    public String getResponse4() {
        return response4;
    }

    public void setResponse4(String response4) {
        this.response4 = response4;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Question{" +
                "crtNo=" + crtNo +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", type=" + type +
                ", weightResponse1=" + weightResponse1 +
                ", response1='" + response1 + '\'' +
                ", weightResponse2=" + weightResponse2 +
                ", response2='" + response2 + '\'' +
                ", weightResponse3=" + weightResponse3 +
                ", response3='" + response3 + '\'' +
                ", weightResponse4=" + weightResponse4 +
                ", response4='" + response4 + '\'' +
                ", id=" + id +
                '}';
    }
}
