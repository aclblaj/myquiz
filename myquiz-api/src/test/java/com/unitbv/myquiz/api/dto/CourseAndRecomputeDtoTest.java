package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseAndRecomputeDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void courseUpsertMapsOnlyWritableCourseFields() {
        CourseUpsertDto input = new CourseUpsertDto("Algorithms", "Core course", "2026-2027", "1");

        CourseDto mapped = input.toCourseDto();

        assertEquals(null, mapped.getId());
        assertEquals("Algorithms", mapped.getCourse());
        assertEquals("Core course", mapped.getDescription());
        assertEquals("2026-2027", mapped.getUniversityYear());
        assertEquals("1", mapped.getSemester());
        assertEquals(0, mapped.getQuestionBankCount());
    }

    @Test
    void courseUpsertCanBeBuiltFromResponseWithoutReadOnlyFields() {
        CourseDto response = new CourseDto(7L, "Algorithms", "Core course", "2026-2027", "1");
        response.setQuestionBankCount(4);

        CourseUpsertDto input = CourseUpsertDto.from(response);

        assertNotNull(input);
        assertEquals("Algorithms", input.getCourse());
        assertEquals("Core course", input.getDescription());
    }

    @Test
    void recomputeSummaryIsImmutableAndHasStableJsonShape() throws Exception {
        DuplicateRecomputeSummaryDto summary = new DuplicateRecomputeSummaryDto(10, 6, 4, 2, 1, 3);
        String json = objectMapper.writeValueAsString(summary);
        DuplicateRecomputeSummaryDto restored = objectMapper.readValue(json, DuplicateRecomputeSummaryDto.class);

        assertEquals(10, restored.totalQuestions());
        assertEquals(2, restored.duplicateLinksRemoved());
        assertThrows(NoSuchMethodException.class, () -> DuplicateRecomputeSummaryDto.class.getMethod("setTotalQuestions", int.class));
    }

    @Test
    void courseRecomputeResultCanBeCreatedFromSummary() {
        DuplicateRecomputeSummaryDto summary = new DuplicateRecomputeSummaryDto(10, 6, 4, 2, 1, 3);
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-09-15T08:00:00Z");

        CourseDuplicateRecomputeResultDto result = CourseDuplicateRecomputeResultDto.from(
                7L, "Algorithms", startedAt, startedAt.plusSeconds(2), 2000, summary);

        assertEquals(7L, result.getCourseId());
        assertEquals(10, result.getTotalQuestions());
        assertEquals(3, result.getDuplicateErrorsCreated());
        assertEquals(2000, result.getDurationMs());
    }
}
