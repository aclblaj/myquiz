package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionErrorServiceTest {

    @Mock
    private QuestionErrorRepository questionErrorRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AuthorService authorService;

    @Mock
    private QuestionBankService questionBankService;

    @Mock
    private CourseService courseService;

    @Test
    void countErrorsByAuthorAndQuestionBank_ignoresDuplicateValidationErrors() {
        QuestionErrorService service = new QuestionErrorService(
                questionErrorRepository,
                questionRepository,
                authorService,
                questionBankService,
                courseService
        );

        when(questionErrorRepository.findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(5L, 9L))
                .thenReturn(List.of(
                        questionError(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS + " [Duplicate title]", 1),
                        questionError(MyUtil.MISSING_ANSWER, 2),
                        questionError(MyUtil.DATATYPE_ERROR, 3)
                ));

        int count = service.countErrorsByAuthorAndQuestionBank(9L, 5L);

        assertEquals(2, count);
    }

    @Test
    void getErrorsByQuestionBankAndAuthor_returnsOnlyNonDuplicateErrors() {
        QuestionErrorService service = new QuestionErrorService(
                questionErrorRepository,
                questionRepository,
                authorService,
                questionBankService,
                courseService
        );

        when(questionErrorRepository.findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(5L, 9L))
                .thenReturn(List.of(
                        questionError(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS + " [Duplicate answer]", 4),
                        questionError(MyUtil.MISSING_ANSWER, 5)
                ));

        var errors = service.getErrorsByQuestionBankAndAuthor(5L, 9L);

        assertEquals(1, errors.size());
        assertEquals(MyUtil.MISSING_ANSWER, errors.getFirst().getDescription());
        assertEquals(5, errors.getFirst().getRow());
    }

    @Test
    void getErrorById_hidesDuplicateValidationErrors() {
        QuestionErrorService service = new QuestionErrorService(
                questionErrorRepository,
                questionRepository,
                authorService,
                questionBankService,
                courseService
        );

        when(questionErrorRepository.findById(12L))
                .thenReturn(java.util.Optional.of(questionError(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS + " [Duplicate title]", 8)));

        assertNull(service.getErrorById(12L));
    }

    private QuestionError questionError(String description, int rowNumber) {
        Author author = new Author();
        author.setId(9L);
        author.setName("Author A");

        QuestionBank questionBank = new QuestionBank();
        questionBank.setId(5L);
        questionBank.setName("QB");

        QuestionBankAuthor questionBankAuthor = new QuestionBankAuthor();
        questionBankAuthor.setId(15L);
        questionBankAuthor.setAuthor(author);
        questionBankAuthor.setQuestionBank(questionBank);

        Question question = new Question();
        question.setId((long) rowNumber);
        question.setCrtNo(rowNumber);
        question.setQuestionBankAuthor(questionBankAuthor);

        return new QuestionError(question, description, rowNumber);
    }
}

