package com.unitbv.myquiz.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class QuizAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToOne()
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    private String source;

    @OneToMany(mappedBy = "quizAuthor", cascade = CascadeType.ALL)
    Set<QuizError> quizErrors;

    @OneToMany(mappedBy = "quizAuthor", cascade = CascadeType.ALL)
    Set<Question> questions;

    public QuizAuthor() {
    }

    public Long getId() {
        return id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public Set<QuizError> getQuizErrors() {
        return quizErrors;
    }

    public void setQuizErrors(Set<QuizError> quizErrors) {
        this.quizErrors = quizErrors;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }
}
