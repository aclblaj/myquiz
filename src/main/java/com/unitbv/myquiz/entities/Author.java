package com.unitbv.myquiz.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class Author {

    String name;
    String initials;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    Set<AuthorError> authorErrors;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    Set<Question> questions;

    public Author() {
    }

    public Author(String name, String initials) {
        this.name = name;
        this.initials = initials;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }

    public Set<AuthorError> getAuthorErrors() {
        return authorErrors;
    }

    public void setAuthorErrors(Set<AuthorError> authorErrors) {
        this.authorErrors = authorErrors;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String author) {
        this.name = author;
    }

    public String getName() {
        return name;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }
    public String getInitials() {
        return initials;
    }

    @Override
    public String toString() {
        return "Author{" +
                "name='" + name + '\'' +
                ", initials='" + initials + '\'' +
                ", id=" + id +
//                ", authorErrors=" + authorErrors +
                '}';
    }
}
