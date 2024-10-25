package com.unitbv.myquiz.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String course;

    private Long year;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    Set<QuizAuthor> quizAuthors = new HashSet<>();

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
}
