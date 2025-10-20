package com.unitbv.myquiz.entities;

import com.unitbv.myquiz.util.TemplateType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
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

    @Enumerated(EnumType.STRING)
    private TemplateType templateType;

    @OneToMany(mappedBy = "quizAuthor", cascade = CascadeType.ALL)
    Set<QuizError> quizErrors = new HashSet<>();

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

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public String getName() {
        if (author != null) {
            return author.getName();
        }
        return null;
    }

    public String getAuthorName() {
        return author != null ? author.getName() : null;
    }

    public String getDescription() {
        return source;
    }

    public int getRow() {
        // Return a unique identifier or row number for this author
        return id != null ? id.intValue() : -1;
    }
}
