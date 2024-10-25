package com.unitbv.myquiz.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class QuizError {
    @Column(length = 512)
    String description;
    Integer rowNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long authorErrorId) {
        this.id = authorErrorId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_author_id")
    QuizAuthor quizAuthor;

    public QuizError() {
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

    public QuizAuthor getQuizAuthor() {
        return quizAuthor;
    }

    public void setQuizAuthor(QuizAuthor quizAuthor) {
        this.quizAuthor = quizAuthor;
    }

}
