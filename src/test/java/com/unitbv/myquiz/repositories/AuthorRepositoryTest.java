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
        author.setName("test name 20 dec");
        author.setInitials("TN");

        Set<AuthorError> errors = new HashSet<>();

        AuthorError authorError = new AuthorError();
        authorError.setDescription("test error");
//        authorErrorRepository.save(authorError);

        errors.add(authorError);

        authorError = new AuthorError();
        authorError.setDescription("test error 2");
//        authorErrorRepository.save(authorError);

        errors.add(authorError);

        author = authorRepository.save(author);
        assertNotNull(author.getId());

        System.out.println("id: " + author.getId());
        System.out.println("author: " + author.toString());

        //authorRepository.delete(author);
    }
}