package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.QuestionBankAuthorService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import com.unitbv.myquiz.app.services.QuestionCorrectionService;
import com.unitbv.myquiz.app.services.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;
    @Mock
    private AuthorService authorService;
    @Mock
    private CourseService courseService;
    @Mock
    private QuestionBankService questionBankService;
    @Mock
    private QuestionBankAuthorService questionBankAuthorService;
    @Mock
    private QuestionCorrectionService questionCorrectionService;

    private QuestionController controller;

    @BeforeEach
    void setUp() {
        controller = new QuestionController(
                questionService,
                authorService,
                courseService,
                questionBankService,
                questionBankAuthorService,
                questionCorrectionService
        );
    }

    @Test
    void updateQuestion_returnsBadRequestWhenPayloadMissing() {
        var response = controller.updateQuestion(5L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(questionService);
    }

    @Test
    void correctGrammar_returnsBadRequestWhenPayloadMissing() {
        var response = controller.correctGrammar(7L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(questionCorrectionService);
    }

    @Test
    void getSampleQuestion_returnsBadRequestForUnknownType() {
        var response = controller.getSampleQuestion("not-a-type");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getQuestionsByAuthorAndQuestionBank_buildsSharedFilterResponse() {
        QuestionBankDto questionBankDto = new QuestionBankDto();
        questionBankDto.setId(11L);
        questionBankDto.setName("Core bank");
        questionBankDto.setCourse("Networks");
        questionBankDto.setCourseId(42L);

        QuestionBankAuthor questionBankAuthor = new QuestionBankAuthor();
        questionBankAuthor.setId(77L);

        QuestionDto questionDto = new QuestionDto();
        questionDto.setId(101L);
        questionDto.setTitle("Q1");
        questionDto.setText("Question text");
        questionDto.setType(QuestionType.MULTICHOICE);

        when(questionBankAuthorService.getQuestionBankAuthorByQuestionBankIdAndAuthorId(11L, 9L))
                .thenReturn(Optional.of(questionBankAuthor));
        when(questionBankService.getQuestionBankById(11L)).thenReturn(questionBankDto);
        when(questionService.getQuestionDtosForQuestionBankAuthor(77L)).thenReturn(List.of(questionDto));
        when(authorService.getAuthorsByCourse("Networks")).thenReturn(List.of(AuthorInfo.builder().id(9L).name("Alice").build()));
        CourseDto courseDto = new CourseDto();
        courseDto.setId(42L);
        courseDto.setCourse("Networks");
        when(courseService.getAllCourses()).thenReturn(List.of(courseDto));

        var response = controller.getQuestionsByAuthorAndQuestionBank(9L, 11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        QuestionFilterResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getQuestions().size());
        assertEquals(9L, body.getSelectedAuthorId());
        assertEquals("Networks", body.getSelectedCourse());
        assertEquals(1, body.getAuthors().size());
        assertEquals(1, body.getAllCourses().size());
        assertEquals("Networks", body.getAllCourses().getFirst().getName());
    }

    @Test
    void listQuestionsFiltered_resolvesCourseFromQuestionBankWhenCourseMissing() {
        QuestionFilterRequestDto request = new QuestionFilterRequestDto();
        request.setAuthorId(202L);
        request.setQuestionBank(24L);

        QuestionBankDto questionBankDto = new QuestionBankDto();
        questionBankDto.setId(24L);
        questionBankDto.setCourse("Databases");
        questionBankDto.setCourseId(10L);

        QuestionFilterResponseDto serviceResponse = new QuestionFilterResponseDto();
        serviceResponse.setQuestions(List.of());

        when(questionBankService.getQuestionBankById(24L)).thenReturn(questionBankDto);
        when(questionService.getQuestionsFiltered("Databases", 202L, 1, 10, 24L, null)).thenReturn(serviceResponse);

        var response = controller.listQuestionsFiltered(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getSelectedCourseId());
        verify(questionService).getQuestionsFiltered(eq("Databases"), eq(202L), eq(1), eq(10), eq(24L), isNull());
    }
}
