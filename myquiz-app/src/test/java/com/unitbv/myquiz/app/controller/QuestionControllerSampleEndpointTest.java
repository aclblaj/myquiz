package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.FilterOptionsService;
import com.unitbv.myquiz.app.services.QuestionBankAuthorService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import com.unitbv.myquiz.app.services.QuestionCorrectionService;
import com.unitbv.myquiz.app.services.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint/contract test for GET /api/questions/sample.
 * Drives the controller through the real HTTP layer with MockMvc while the
 * collaborating services are mocked, so no database or running server is needed.
 */
@ExtendWith(MockitoExtension.class)
class QuestionControllerSampleEndpointTest {

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
    @Mock
    private FilterOptionsService filterOptionsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QuestionController controller = new QuestionController(
                questionService,
                authorService,
                courseService,
                questionBankService,
                questionBankAuthorService,
                questionCorrectionService,
                filterOptionsService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSample_withoutTypeParameter_returnsMultichoiceContract() throws Exception {
        // The default value of the "type" query parameter is MULTICHOICE.
        when(authorService.extractInitials(ControllerSettings.DEFAULT_AUTHOR)).thenReturn("MM");

        mockMvc.perform(get("/api/questions/sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("MULTICHOICE"))
                .andExpect(jsonPath("$.title").value("SQL Query Basics"))
                .andExpect(jsonPath("$.chapter").value("Databases"))
                .andExpect(jsonPath("$.response1").value("SELECT"))
                .andExpect(jsonPath("$.weightResponse1").value(100.0))
                // The mocked service must actually be wired into the response payload.
                .andExpect(jsonPath("$.author.name").value(ControllerSettings.DEFAULT_AUTHOR))
                .andExpect(jsonPath("$.author.initials").value("MM"));

        verify(authorService).extractInitials(ControllerSettings.DEFAULT_AUTHOR);
    }

    @Test
    void getSample_withTrueFalseTypeIgnoringCase_returnsTrueFalseContract() throws Exception {
        // Lower-case input also proves the endpoint upper-cases before resolving the enum.
        when(authorService.extractInitials(ControllerSettings.DEFAULT_AUTHOR)).thenReturn("MM");

        mockMvc.perform(get("/api/questions/sample").param("type", "truefalse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRUEFALSE"))
                .andExpect(jsonPath("$.title").value("SQL Statement Usage"))
                .andExpect(jsonPath("$.response1").value("True"))
                .andExpect(jsonPath("$.weightTrue").value(100.0))
                .andExpect(jsonPath("$.weightFalse").value(-100.0));
    }

    @Test
    void getSample_withUnknownType_returnsBadRequest() throws Exception {
        // An unmapped type must not reach the services; the contract returns 400.
        mockMvc.perform(get("/api/questions/sample").param("type", "NON_EXISTENT"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authorService);
    }
}
