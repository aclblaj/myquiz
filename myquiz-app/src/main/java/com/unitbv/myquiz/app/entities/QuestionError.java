package com.unitbv.myquiz.app.entities;

import jakarta.persistence.*;

/**
 * Junction entity representing the many-to-many relationship between Question and QuizError.
 * Allows tracking multiple errors per question.
 */
@Entity
@Table(name = "question_error")
public class QuestionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_error_id", nullable = false)
    private QuizError quizError;

    public QuestionError() {
    }

    public QuestionError(Question question, QuizError quizError) {
        this.question = question;
        this.quizError = quizError;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public QuizError getQuizError() {
        return quizError;
    }

    public void setQuizError(QuizError quizError) {
        this.quizError = quizError;
    }
}

