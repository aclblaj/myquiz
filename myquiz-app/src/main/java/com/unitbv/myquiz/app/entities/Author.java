package com.unitbv.myquiz.app.entities;

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
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "author_gen")
    @SequenceGenerator(name = "author_gen", sequenceName = "author_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;
    private String name;
    private String initials;

    @OneToMany(mappedBy = "author")
    private Set<QuizAuthor> quizAuthors = new HashSet<>();

    public Author() {
    }

    public Author(String name, String initials) {
        this.name = name;
        this.initials = initials;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
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

    @Override
    public String toString() {
        return "Author{" +
                "name='" + name + '\'' +
                ", initials='" + initials + '\'' +
                ", id=" + id +
                '}';
    }
}
