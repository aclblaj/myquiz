package com.unitbv.myquiz.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class QuizError {
    @Column(length = 512)
    private String description;
    private Integer rowNumber;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_error_gen")
    @SequenceGenerator(name = "quiz_error_gen", sequenceName = "quiz_error_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_author_id")
    private QuizAuthor quizAuthor;

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

    public Long getId() {
        return id;
    }

    public void setId(Long authorErrorId) {
        this.id = authorErrorId;
    }

    public QuizAuthor getQuizAuthor() {
        return quizAuthor;
    }

    public void setQuizAuthor(QuizAuthor quizAuthor) {
        this.quizAuthor = quizAuthor;
    }

}
