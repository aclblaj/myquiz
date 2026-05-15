package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import com.unitbv.myquiz.app.testutil.TestFixtureData;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AuthorRepositoryTest {

    Logger logger = LoggerFactory.getLogger(AuthorRepositoryTest.class);

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    void saveAuthor() {
        Author author = testEntityFactory.createAuthor(TestFixtureData.AUTHOR_NAME, TestFixtureData.AUTHOR_INITIALS);
        assertNotNull(author.getId());
        authorRepository.findById(author.getId()).ifPresent(authorRepository::delete);
        logger.atInfo().addArgument(author).log("Author saved and deleted: {}");
    }

}
