package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.mapper.QuestionDuplicateMapper;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceFilterQuizzesTest {

    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private QuestionBankAuthorRepository questionBankAuthorRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private QuestionDuplicateRepository questionDuplicateRepository;
    @Mock
    private QuestionErrorRepository questionErrorRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private QuestionDuplicateMapper questionDuplicateMapper;
    @Mock
    private QuestionDuplicationService questionDuplicationService;

    private QuestionBankService questionBankService;

    @BeforeEach
    void setUp() {
        questionBankService = new QuestionBankService(
                questionBankRepository, questionBankAuthorRepository, questionRepository, questionErrorRepository, questionDuplicateRepository, authorRepository, courseService,
                                                      questionMapper, questionDuplicateMapper
        );
    }

    @Test
    void filterQuestionBanks_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> questionBankService.filterQuestionBanks(null));
    }

    @Test
    void filterQuestionBanks_invalidPageAndPageSize_usesDefaults() {
        stubCommonFilterDependencies();

        QuestionBankFilterRequestDto input = new QuestionBankFilterRequestDto();
        input.setPage(0);
        input.setPageSize(0);
        input.setCourseId(null);

        when(questionBankRepository.findAll()).thenReturn(List.of(
                questionBank(2L, "Q2", ServiceTestData.COURSE, StudyYear.Y2026_2027),
                questionBank(1L, ServiceTestData.QUESTION_BANK_NAME, ServiceTestData.COURSE, StudyYear.Y2026_2027)
        ));

        QuestionBankFilterResponseDto result = questionBankService.filterQuestionBanks(input);

        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getQuestionBanks().size());
        assertEquals(0L, result.getQuestionBanks().getFirst().getNumberOfDuplicates());
        verify(questionBankRepository).findAll();
    }

    @Test
    void filterQuestionBanks_courseId_usesCourseFilterSpecification() {
        stubCommonFilterDependencies();

        QuestionBankFilterRequestDto input = new QuestionBankFilterRequestDto();
        input.setPage(1);
        input.setPageSize(10);
        input.setCourseId(1L);

        when(questionBankRepository.findAll(any(Specification.class))).thenReturn(List.of(
                questionBank(1L, ServiceTestData.QUESTION_BANK_NAME, ServiceTestData.COURSE, StudyYear.Y2026_2027)
        ));

        QuestionBankFilterResponseDto result = questionBankService.filterQuestionBanks(input);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getQuestionBanks().size());
        assertEquals(ServiceTestData.COURSE, result.getQuestionBanks().getFirst().getCourse());
        verify(questionBankRepository).findAll(any(Specification.class));
        verify(questionBankRepository, never()).findAll();
    }

    @Test
    void filterQuestionBanks_pageOutOfRange_clampsToLastPage() {
        stubCommonFilterDependencies();

        QuestionBankFilterRequestDto input = new QuestionBankFilterRequestDto();
        input.setPage(3);
        input.setPageSize(2);
        input.setCourseId(null);

        when(questionBankRepository.findAll()).thenReturn(
                List.of(
                        questionBank(1L, ServiceTestData.QUESTION_BANK_NAME, ServiceTestData.COURSE, StudyYear.Y2026_2027),
                        questionBank(2L, "Q2", ServiceTestData.COURSE, StudyYear.Y2026_2027),
                        questionBank(3L, "Q3", ServiceTestData.COURSE, StudyYear.Y2026_2027)
                )
        );

        QuestionBankFilterResponseDto result = questionBankService.filterQuestionBanks(input);

        assertEquals(2, result.getTotalPages());
        assertEquals(2, result.getPage());
        assertEquals(1, result.getQuestionBanks().size());
        assertEquals(3L, result.getQuestionBanks().getFirst().getId());
        assertTrue(result.getQuestionBanks().stream().allMatch(dto -> ServiceTestData.COURSE.equals(dto.getCourse())));
    }

    private QuestionBank questionBank(Long id, String name, String courseName, StudyYear studyYear) {
        Course course = new Course();
        course.setId(id);
        course.setCourse(courseName);

        QuestionBank questionBank = new QuestionBank();
        questionBank.setId(id);
        questionBank.setName(name);
        questionBank.setCourse(course);
        questionBank.setStudyYear(studyYear);
        return questionBank;
    }

    private void stubCommonFilterDependencies() {
        when(questionRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(courseService.getAllCourses()).thenReturn(List.of());
    }
}


