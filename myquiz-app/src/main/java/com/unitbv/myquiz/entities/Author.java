package com.unitbv.myquiz.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String name;
    String initials;

    @OneToMany(mappedBy = "author")
    Set<QuizAuthor> quizAuthors = new HashSet<>();

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
//                ", quizzes=" + quizzes +
                '}';
    }
}
