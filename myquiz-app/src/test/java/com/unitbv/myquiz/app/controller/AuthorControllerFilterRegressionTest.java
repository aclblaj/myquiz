package com.unitbv.myquiz.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.QuestionBankAuthorService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthorControllerFilterRegressionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    private AuthorService authorService;

    @Mock
    private QuestionBankAuthorService questionBankAuthorService;

    @Mock
    private QuestionBankService questionBankService;

    @Mock
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        AuthorController controller = new AuthorController(
                authorService,
                questionBankAuthorService,
                questionBankService,
                courseService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).addPlaceholderValue(
                "FRONTEND_URL",
                "http://localhost:3000"
        ).build();
    }

    @Test
    void listAuthors_usesServicePageContent_withoutExtraAuthorRemapping() throws Exception {
        AuthorFilterRequestDto input = new AuthorFilterRequestDto();
        input.setCourseId(1L);
        input.setPage(1);
        input.setPageSize(10);

        AuthorDto authorDto = AuthorDto.builder().id(1L).name("Alice Author").initials("AA").build();
        authorDto.setCourse("Algorithms");
        Page<AuthorDto> page = new PageImpl<>(List.of(authorDto));

        when(courseService.getAllCourses()).thenReturn(List.of(new CourseDto(
                1L,
                "Algorithms"
        )));
        when(courseService.getCourseName(1L)).thenReturn("Algorithms");
        when(authorService.findPaginatedFiltered(
                eq("Algorithms"),
                isNull(),
                isNull(),
                eq(1),
                eq(10),
                eq("name"),
                eq("desc")
        )).thenReturn(page);
        when(questionBankAuthorService.getAuthorDtosByCourse("Algorithms")).thenReturn(List.of(new AuthorInfo(
                1L,
                "Alice Author",
                null
        )));
        when(questionBankService.getQuestionBankInfoByCourse("Algorithms")).thenReturn(List.of(new QuestionBankInfo(
                11L,
                "QB 1",
                "Algorithms"
        )));

        mockMvc.perform(post("/api/authors/filter").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(input))).andExpect(status().isOk())
               .andExpect(jsonPath("$.authors.length()").value(1)).andExpect(jsonPath("$.authors[0].id").value(1)).andExpect(jsonPath("$.authors[0].name").value("Alice Author"))
               .andExpect(jsonPath("$.selectedCourse").value("Algorithms")).andExpect(jsonPath("$.totalElements").value(1));

        verify(
                authorService,
                never()
        ).getAuthorWithQuestionBankStats(
                anyLong(),
                anyString()
        );
    }
}



