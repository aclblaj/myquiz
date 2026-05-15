package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import com.unitbv.myquiz.app.testutil.TestFixtureData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class QuestionRepositoryTest {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    void findById() {
        TestEntityFactory.QuestionFixture fixture = addTestQuestion();
        try {
            Question question = questionRepository.findById(fixture.question().getId()).orElse(null);
            assertNotNull(question);
            assertEquals(fixture.question().getId(), question.getId());
        } finally {
            testEntityFactory.cleanupQuestionFixture(fixture);
        }

        Question deletedQuestion = questionRepository.findById(fixture.question().getId()).orElse(null);
        assertNull(deletedQuestion);
    }

    private TestEntityFactory.QuestionFixture addTestQuestion() {
        return testEntityFactory.createQuestionFixture(
                TestFixtureData.questionSpecBuilder()
                        .type(QuestionType.MULTICHOICE)
                        .build()
        );
    }


}
