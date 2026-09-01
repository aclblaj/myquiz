package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.DuplicateRecomputeHistoryRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.properties",
        properties = {
                "myquiz.tasks.duplicate-check.core-pool-size=80",
                "myquiz.tasks.duplicate-check.max-pool-size=80",
                "myquiz.tasks.duplicate-check.queue-capacity=20000",
                "logging.level.com.unitbv.myquiz.app.services.QuestionDuplicationService=DEBUG"
        }
)
@Transactional
class CourseServiceTest {

    @Autowired
    CourseService courseService;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    QuestionBankRepository questionBankRepository;

    @Autowired
    QuestionDuplicateRepository questionDuplicateRepository;

    @Autowired
    QuestionErrorRepository questionErrorRepository;

    @Autowired
    DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    void createCourseCreatesNewCourse() {
        CourseDto dto = courseDto("Algorithms");

        CourseDto saved = courseService.createCourse(dto);

        assertNotNull(saved.getId());
        assertEquals("Algorithms", saved.getCourse());
    }

    @Test
    void createCourseReturnsExistingCourseWhenAlreadyPresent() {
        CourseDto first = courseService.createCourse(courseDto("Databases"));
        CourseDto second = courseService.createCourse(courseDto("Databases"));

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void createCourseRejectsNullDto() {
        assertThrows(IllegalArgumentException.class, () -> courseService.createCourse(null));
    }

    @Test
    void createCourseRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> courseService.createCourse(courseDto(" ")));
    }

    @Test
    void createCourseIfNotExistsReturnsExistingCourse() {
        CourseDto existing = courseService.createCourse(courseDto("Networks"));

        CourseDto result = courseService.createCourseIfNotExists(courseDto("Networks"));

        assertEquals(existing.getId(), result.getId());
    }

    @Test
    void getAllCourseNamesReturnsCreatedCourse() {
        courseService.createCourse(courseDto("Software Engineering"));

        List<String> names = courseService.getAllCourseNames();

        assertTrue(names.contains("Software Engineering"));
    }

    @Test
    void getCourseNameReturnsEmptyStringForMissingId() {
        assertEquals("", courseService.getCourseName(-1L));
    }

    @Test
    void findByIdReturnsCourseDto() {
        CourseDto created = courseService.createCourse(courseDto("Operating Systems"));

        CourseDto found = courseService.findById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Operating Systems", found.getCourse());
    }

    @Test
    void findByIdRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> courseService.findById(null));
    }

    @Test
    void updateCourseChangesPersistedValues() {
        CourseDto created = courseService.createCourse(courseDto("Old Course"));
        CourseDto update = courseDto("New Course");
        update.setDescription("Updated description");

        courseService.updateCourse(created.getId(), update);

        Course reloaded = courseRepository.findById(created.getId()).orElseThrow();
        assertEquals("New Course", reloaded.getCourse());
        assertEquals("Updated description", reloaded.getDescription());
    }

    @Test
    void deleteCourseByIdRemovesCourse() {
        CourseDto created = courseService.createCourse(courseDto("Discrete Math"));

        courseService.deleteCourseById(created.getId());

        assertFalse(courseRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteCourseByIdRejectsMissingCourse() {
        assertThrows(IllegalArgumentException.class, () -> courseService.deleteCourseById(-1L));
    }

    @Test
    void getDuplicateStatisticsRejectsNullCourseId() {
        assertThrows(IllegalArgumentException.class, () -> courseService.getDuplicateStatistics((Long) null));
    }

    @Test
    void saveAndReadRecomputeHistoryRoundTrips() {
        CourseDto created = courseService.createCourse(courseDto("Compilers"));
        CourseDuplicateRecomputeResultDto result = new CourseDuplicateRecomputeResultDto();
        result.setCourseId(created.getId());
        result.setCourseName(created.getCourse());
        result.setTotalQuestions(3);
        result.setMultichoiceQuestions(2);
        result.setTruefalseQuestions(1);
        result.setDuplicateLinksRemoved(1);
        result.setDuplicateErrorsRemoved(2);
        result.setDuplicateErrorsCreated(3);

        DuplicateRecomputeHistoryDto saved = courseService.saveRecomputeHistory(result, "levenshtein", created.getId(), null, null);
        List<DuplicateRecomputeHistoryDto> history = courseService.getRecomputeHistory();

        assertNotNull(saved.getId());
        assertFalse(history.isEmpty());
        assertEquals(created.getId(), history.get(0).getCourseId());
    }

    private CourseDto courseDto(String name) {
        return ServiceTestData.courseDtoBuilder()
                              .course(name)
                              .description(name + " description")
                              .semester("1")
                              .universityYear("1")
                              .build();
    }
}
