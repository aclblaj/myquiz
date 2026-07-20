package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.services.MyUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDtoEnricherTest {

    private final QuestionDtoEnricher enricher = new QuestionDtoEnricher(new QuestionDuplicateMapper());

    @Test
    void enrichWithErrors_excludesDuplicateValidationErrorsFromErrorList() {
        Question question = new Question();
        question.setId(10L);
        question.setTitle("Primary Question");
        question.setCrtNo(7);

        QuestionError duplicateError = new QuestionError(question, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS + " (Primary Question)", 7);
        QuestionError visibleError = new QuestionError(question, MyUtil.MISSING_ANSWER, 7);
        question.setQuestionErrors(List.of(duplicateError, visibleError));

        Question duplicateQuestion = new Question();
        duplicateQuestion.setId(11L);
        duplicateQuestion.setTitle("Duplicate Question");
        duplicateQuestion.setText("Same text");
        duplicateQuestion.setCrtNo(9);

        QuestionDuplicate duplicateLink = new QuestionDuplicate();
        duplicateLink.setId(100L);
        duplicateLink.setQuestion(question);
        duplicateLink.setDuplicateQuestion(duplicateQuestion);
        question.setDuplicateLinks(List.of(duplicateLink));

        QuestionDto dto = new QuestionDto();
        enricher.enrichWithErrors(dto, question);

        assertEquals(1, dto.getErrors().size());
        assertEquals(MyUtil.MISSING_ANSWER, dto.getErrors().getFirst().getDescription());
        assertEquals(1, dto.getDuplicateCount());
        assertEquals(1, dto.getDuplicates().size());
        assertTrue(dto.getDuplicates().stream().anyMatch(dup -> dup.getQuestionId().equals(11L)));
    }
}

