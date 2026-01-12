package com.unitbv.myquiz.app.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_gen")
    @SequenceGenerator(name = "quiz_gen", sequenceName = "quiz_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;

    private String course;

    private Long year;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private Set<QuizAuthor> quizAuthors = new HashSet<>();

    public Quiz() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Long getYear() {
        return year;
    }

    public void setYear(Long year) {
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<QuizAuthor> getQuizAuthors() {
        return quizAuthors;
    }

    public void setQuizAuthors(Set<QuizAuthor> quizAuthors) {
        this.quizAuthors = quizAuthors;
    }

    public java.util.List<Question> getQuestionsMULTICHOICE() {
        // Placeholder: implement logic to return MULTICHOICE questions
        return new java.util.ArrayList<>();
    }

    public java.util.List<Question> getQuestionsTRUEFALSE() {
        // Placeholder: implement logic to return TRUEFALSE questions
        return null;
    }

    public java.util.List<QuizAuthor> getAuthorErrors() {
        // Placeholder: implement logic to return author errors
        return new java.util.ArrayList<>();
    }
}
