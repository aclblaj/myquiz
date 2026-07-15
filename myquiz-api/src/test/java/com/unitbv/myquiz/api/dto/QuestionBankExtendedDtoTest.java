package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionBankExportDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesDuplicateQuestionsUsingQuestionDuplicateDtoShape() throws Exception {
        QuestionDuplicateDto duplicate = new QuestionDuplicateDto();
        duplicate.setQuestionId(42L);
        duplicate.setTitle("Duplicate title");
        duplicate.setText("Duplicate text");
        duplicate.setRow(7);
        duplicate.setCause("Title: 'test' found as substring into 'test extended'");

        QuestionBankExportAuthorSectionDto authorSection = new QuestionBankExportAuthorSectionDto();
        authorSection.setDuplicateQuestions(List.of(duplicate));

        String json = objectMapper.writeValueAsString(authorSection);

        assertTrue(json.contains("\"duplicateQuestions\""));
        assertTrue(json.contains("\"questionId\":42"));
        assertTrue(json.contains("\"title\":\"Duplicate title\""));
        assertTrue(json.contains("\"text\":\"Duplicate text\""));
        assertTrue(json.contains("\"row\":7"));
        assertTrue(json.contains("\"cause\":\"Title: 'test' found as substring into 'test extended'\""));
    }

    @Test
    void roundTripsExtendedDtoWithAuthorSections() throws Exception {
        QuestionDuplicateDto duplicate = new QuestionDuplicateDto();
        duplicate.setQuestionId(9L);
        duplicate.setTitle("Nested duplicate");

        QuestionBankExportAuthorSectionDto authorSection = new QuestionBankExportAuthorSectionDto();
        authorSection.setDuplicateQuestions(List.of(duplicate));

        QuestionBankExportDto dto = new QuestionBankExportDto();
        dto.setAuthorSections(List.of(authorSection));

        String json = objectMapper.writeValueAsString(dto);
        QuestionBankExportDto restored = objectMapper.readValue(json, QuestionBankExportDto.class);

        assertEquals(1, restored.getAuthorSections().size());
        assertEquals(1, restored.getAuthorSections().getFirst().getDuplicateQuestions().size());
        assertEquals(9L, restored.getAuthorSections().getFirst().getDuplicateQuestions().getFirst().getQuestionId());
        assertEquals("Nested duplicate", restored.getAuthorSections().getFirst().getDuplicateQuestions().getFirst().getTitle());
    }
}

