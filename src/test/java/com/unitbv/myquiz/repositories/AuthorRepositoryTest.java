package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.AuthorError;
import com.unitbv.myquiz.services.MyUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthorRepositoryTest {

    Logger logger = LoggerFactory.getLogger(AuthorRepositoryTest.class);

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


        logger.atInfo().addArgument(author.getId()).log("Author saved with id: {}");
        logger.atInfo().addArgument(author).log("Author saved: {}");

        authorRepository.delete(author);
    }

    // create test to save an author having 3 author errors

    @Test
    void saveAuthorWithErrors() {
        Author author = new Author();
        author.setName("Max Mustermann");
        author.setInitials("MM");

        Set<AuthorError> errors = new HashSet<>();

        AuthorError authorError1 = new AuthorError();
        authorError1.setDescription(MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
        authorError1.setAuthor(author);

        errors.add(authorError1);

        AuthorError authorError2 = new AuthorError();
        authorError2.setDescription(MyUtil.SKIPPED_DUE_TO_ERROR);
        authorError2.setAuthor(author);

        errors.add(authorError2);

        AuthorError authorError3 = new AuthorError();
        authorError3.setDescription(MyUtil.DATATYPE_ERROR);
        authorError3.setAuthor(author);

        errors.add(authorError3);

        author.setAuthorErrors(errors);

        author = authorRepository.save(author);
        assertNotNull(author.getId());


        logger.atInfo().addArgument(author.getId()).log("Author saved with id: {}");
        logger.atInfo().addArgument(author).log("Author saved: {}");
        author.getAuthorErrors().forEach(authorError -> logger.atInfo().addArgument(authorError).log("Author error: {}"));

        assertEquals(3, author.getAuthorErrors().size());

        authorRepository.delete(author);
    }

}