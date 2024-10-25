package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
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