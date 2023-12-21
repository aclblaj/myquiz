package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.AuthorError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthorRepositoryTest {

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    AuthorErrorRepository authorErrorRepository;

    @Test
    void save() {
        Author author = new Author();
        author.setName("Max Mustermann");
        author.setInitials("MM");

        Set<AuthorError> errors = new HashSet<>();

        AuthorError authorError1 = new AuthorError();
        authorError1.setDescription("Duplicate author");
        authorError1.setAuthor(author);

        errors.add(authorError1);

        AuthorError authorError2 = new AuthorError();
        authorError2.setDescription("Missing value");
        authorError2.setAuthor(author);

        errors.add(authorError2);

        author.setAuthorErrors(errors);

        author = authorRepository.save(author);
        assertNotNull(author.getId());

        System.out.println("id: " + author.getId());
        System.out.println("author: " + author.toString());

        authorRepository.delete(author);
    }

    // create test to save an author having 3 author errors


}