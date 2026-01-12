package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Author;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthorRepositoryTest {

    Logger logger = LoggerFactory.getLogger(AuthorRepositoryTest.class);

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    QuizErrorRepository quizErrorRepository;

    @Test
    void saveAuthor() {
        Author author = new Author();
        author.setName("Monika Mustermann");
        author.setInitials("EM");
        author = authorRepository.save(author);
        assertNotNull(author.getId());
        authorRepository.delete(author);
        logger.atInfo().addArgument(author).log("Author saved and deleted: {}");
    }

}