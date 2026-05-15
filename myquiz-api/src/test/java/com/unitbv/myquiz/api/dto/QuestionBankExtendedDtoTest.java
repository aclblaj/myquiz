package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionBankExportDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesDuplicateQuestionsUsingQuestionDtoShape() throws Exception {
        QuestionDto duplicate = new QuestionDto();
        duplicate.setId(42L);
        duplicate.setTitle("Duplicate title");
        duplicate.setText("Duplicate text");
        duplicate.setRow(7);

        QuestionBankExportAuthorSectionDto authorSection = new QuestionBankExportAuthorSectionDto();
        authorSection.setDuplicateQuestions(List.of(duplicate));

        String json = objectMapper.writeValueAsString(authorSection);

        assertTrue(json.contains("\"duplicateQuestions\""));
        assertTrue(json.contains("\"id\":42"));
        assertTrue(json.contains("\"title\":\"Duplicate title\""));
        assertTrue(json.contains("\"text\":\"Duplicate text\""));
        assertTrue(json.contains("\"row\":7"));
    }

    @Test
    void roundTripsExtendedDtoWithAuthorSections() throws Exception {
        QuestionDto duplicate = new QuestionDto();
        duplicate.setId(9L);
        duplicate.setTitle("Nested duplicate");

        QuestionBankExportAuthorSectionDto authorSection = new QuestionBankExportAuthorSectionDto();
        authorSection.setDuplicateQuestions(List.of(duplicate));

        QuestionBankExportDto dto = new QuestionBankExportDto();
        dto.setAuthorSections(List.of(authorSection));

        String json = objectMapper.writeValueAsString(dto);
        QuestionBankExportDto restored = objectMapper.readValue(json, QuestionBankExportDto.class);

        assertEquals(1, restored.getAuthorSections().size());
        assertEquals(1, restored.getAuthorSections().getFirst().getDuplicateQuestions().size());
        assertEquals(9L, restored.getAuthorSections().getFirst().getDuplicateQuestions().getFirst().getId());
        assertEquals("Nested duplicate", restored.getAuthorSections().getFirst().getDuplicateQuestions().getFirst().getTitle());
    }
}

