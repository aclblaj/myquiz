package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import com.unitbv.myquiz.app.testutil.TestFixtureData;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class QuestionBankAuthorRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankAuthorRepositoryTest.class);
    @Autowired
    QuestionBankAuthorRepository questionBankAuthorRepository;
    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    void saveQuestionBankAuthor() {
        TestEntityFactory.QuestionBankAuthorFixture fixture = testEntityFactory.createQuestionBankAuthorFixture(
                TestFixtureData.questionBankAuthorSpecBuilder()
                        .build()
        );

        Author author = fixture.author();
        QuestionBank questionBank = fixture.questionBank();
        QuestionBankAuthor questionBankAuthor = fixture.questionBankAuthor();


        questionBankAuthor = questionBankAuthorRepository.save(questionBankAuthor);

        Long authorId = questionBankAuthor.getAuthor().getId();
        Long questionBankId = questionBankAuthor.getQuestionBank().getId();
        log.info("Author id: {}", authorId);
        log.info("QuestionBank id: {}", questionBankId);

        assertNotNull(questionBankAuthor.getId());

        testEntityFactory.cleanupQuestionBankAuthorFixture(new TestEntityFactory.QuestionBankAuthorFixture(author, questionBank, questionBankAuthor));
    }

}
