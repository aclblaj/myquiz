package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateUnlinkRequestDtoSerializesDuplicateQuestionIds() throws Exception {
        DuplicateUnlinkRequestDto dto = new DuplicateUnlinkRequestDto();
        dto.setDuplicateQuestionIds(List.of(11L, 22L));

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"duplicateQuestionIds\":[11,22]"));
    }

    @Test
    void duplicateUnlinkRequestDtoDeserializesDuplicateQuestionIds() throws Exception {
        String json = "{\"duplicateQuestionIds\":[5,7,9]}";

        DuplicateUnlinkRequestDto dto = objectMapper.readValue(json, DuplicateUnlinkRequestDto.class);

        assertEquals(List.of(5L, 7L, 9L), dto.getDuplicateQuestionIds());
    }

    @Test
    void duplicateStatisticsDtoRoundTripsStatisticsPayload() throws Exception {
        DuplicateStatisticsDto dto = new DuplicateStatisticsDto("Networks", 120, 8, 14L);

        String json = objectMapper.writeValueAsString(dto);
        DuplicateStatisticsDto restored = objectMapper.readValue(json, DuplicateStatisticsDto.class);

        assertTrue(json.contains("\"courseName\":\"Networks\""));
        assertTrue(json.contains("\"totalQuestions\":120"));
        assertEquals("Networks", restored.getCourseName());
        assertEquals(120, restored.getTotalQuestions());
        assertEquals(8, restored.getQuestionsWithDuplicateErrors());
        assertEquals(14L, restored.getDuplicateLinks());
    }

    @Test
    void courseDuplicateRecomputeResultDtoRoundTripsPayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseId(3L);
        dto.setCourseName("Algorithms");
        dto.setTotalQuestions(42);
        dto.setMultichoiceQuestions(30);
        dto.setTruefalseQuestions(12);
        dto.setDuplicateLinksRemoved(5);
        dto.setDuplicateErrorsRemoved(4);
        dto.setDuplicateErrorsCreated(2);
        dto.setStartedAt(OffsetDateTime.of(2026, 5, 11, 14, 0, 0, 0, ZoneOffset.ofHours(3)));
        dto.setEndedAt(OffsetDateTime.of(2026, 5, 11, 14, 0, 5, 0, ZoneOffset.ofHours(3)));
        dto.setDurationMs(5000L);

        String json = mapper.writeValueAsString(dto);
        CourseDuplicateRecomputeResultDto restored = mapper.readValue(json, CourseDuplicateRecomputeResultDto.class);

        assertTrue(json.contains("\"courseId\":3"));
        assertTrue(json.contains("\"courseName\":\"Algorithms\""));
        assertTrue(json.contains("\"duplicateLinksRemoved\":5"));
        assertEquals(3L, restored.getCourseId());
        assertEquals("Algorithms", restored.getCourseName());
        assertEquals(42, restored.getTotalQuestions());
        assertEquals(30, restored.getMultichoiceQuestions());
        assertEquals(12, restored.getTruefalseQuestions());
        assertEquals(5, restored.getDuplicateLinksRemoved());
        assertEquals(4, restored.getDuplicateErrorsRemoved());
        assertEquals(2, restored.getDuplicateErrorsCreated());
        assertEquals(
                OffsetDateTime.of(2026, 5, 11, 14, 0, 0, 0, ZoneOffset.ofHours(3)).toInstant(),
                restored.getStartedAt().toInstant()
        );
        assertEquals(
                OffsetDateTime.of(2026, 5, 11, 14, 0, 5, 0, ZoneOffset.ofHours(3)).toInstant(),
                restored.getEndedAt().toInstant()
        );
        assertEquals(5000L, restored.getDurationMs());
    }
}


