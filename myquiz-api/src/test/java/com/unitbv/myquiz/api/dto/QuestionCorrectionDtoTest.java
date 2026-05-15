package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionCorrectionDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesModelUsedField() throws Exception {
        String json = "{\"originalQuestion\":{},\"modelUsed\":\"phi4:mini\"}";

        QuestionCorrectionDto dto = objectMapper.readValue(json, QuestionCorrectionDto.class);

        assertEquals("phi4:mini", dto.getModelUsed());
    }

    @Test
    void deserializesLegacyModelAliasIntoModelUsed() throws Exception {
        String json = "{\"originalQuestion\":{},\"model\":\"llama3.1\"}";

        QuestionCorrectionDto dto = objectMapper.readValue(json, QuestionCorrectionDto.class);

        assertEquals("llama3.1", dto.getModelUsed());
    }

    @Test
    void serializesUsingModelUsedPropertyName() throws Exception {
        QuestionCorrectionDto dto = new QuestionCorrectionDto();
        dto.setOriginalQuestion(new QuestionDto());
        dto.setModelUsed("mistral");

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"modelUsed\":\"mistral\""));
    }
}

