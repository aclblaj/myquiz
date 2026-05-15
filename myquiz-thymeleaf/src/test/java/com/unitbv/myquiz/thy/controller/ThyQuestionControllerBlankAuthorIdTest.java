package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.thy.service.QuestionCorrectionService;
import com.unitbv.myquiz.thy.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ThyQuestionControllerBlankAuthorIdTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private QuestionCorrectionService questionCorrectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ThyQuestionController controller = new ThyQuestionController(sessionService, restTemplate, questionCorrectionService);
        ReflectionTestUtils.setField(controller, "apiBaseUrl", "http://localhost:8888/api");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getQuestionById_acceptsBlankAuthorIdAndTypeQueryParameters() throws Exception {
        QuestionDto question = new QuestionDto();
        question.setId(5806L);
        question.setType(QuestionType.MULTICHOICE);
        question.setTitle("Sample question");

        QuestionBankDto bank = new QuestionBankDto();
        bank.setId(30L);
        bank.setName("QB 30");
        bank.setCourse("BD");

        when(sessionService.validateSessionOrRedirect()).thenReturn(null);
        when(sessionService.getAuthorizationHeader()).thenReturn(new HttpEntity<Void>(new HttpHeaders()));
        when(sessionService.getLoggedInUser()).thenReturn(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QuestionDto.class)))
                .thenReturn(ResponseEntity.ok(question));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QuestionBankDto[].class)))
                .thenReturn(ResponseEntity.ok(new QuestionBankDto[]{bank}));

        mockMvc.perform(get("/questions/5806")
                        .queryParam("course", "BD")
                        .queryParam("questionBankId", "30")
                        .queryParam("authorId", "")
                        .queryParam("type", "")
                        .queryParam("page", "1")
                        .queryParam("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name(ControllerSettings.QUESTION_EDITOR_MULTICHOICE));
    }
}




