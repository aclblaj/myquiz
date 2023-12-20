package com.unitbv.myquiz.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class AuthorError {
    String name;
    String initials;
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
//    @Column(name = "id", nullable = false)
    private Long id;



    @ManyToOne
    @JoinColumn(name = "author_id")
    Author author;

    public AuthorError() {
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return "AuthorError{" +
                "name='" + name + '\'' +
                ", initials='" + initials + '\'' +
                ", description='" + description + '\'' +
                ", rowNumber=" + rowNumber +
                ", id=" + id +
                ", author=" + author +
                '}';
    }
}
