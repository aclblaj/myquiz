package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionUpsertValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    @Test
    void acceptsConsistentMultipleChoiceRequest() {
        QuestionUpsertDto dto = validQuestion();

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsMissingQuestionBankReference() {
        QuestionUpsertDto dto = validQuestion();
        dto.setQuestionBankId(null);
        dto.setQuestionBankName(" ");

        Set<?> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsIncompleteMultipleChoiceAnswers() {
        QuestionUpsertDto dto = validQuestion();
        dto.setResponse4(null);

        Set<?> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    private static QuestionUpsertDto validQuestion() {
        QuestionUpsertDto dto = new QuestionUpsertDto();
        dto.setQuestionBankId(1L);
        dto.setText("Question text");
        dto.setCourse("Course");
        dto.setType(QuestionType.MULTICHOICE);
        dto.setResponse1("A");
        dto.setResponse2("B");
        dto.setResponse3("C");
        dto.setResponse4("D");
        return dto;
    }
}
