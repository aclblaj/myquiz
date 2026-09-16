package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDtoJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesQuestionMetadata() throws Exception {
        QuestionDto dto = new QuestionDto();
        dto.setId(7L);
        dto.setDuplicateCount(2);

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"id\":7"));
        assertTrue(json.contains("\"duplicateCount\":2"));
    }

    @Test
    void preservesQuestionMetadataWhenApiConsumerDeserializesResponse() throws Exception {
        String json = "{\"id\":7,\"text\":\"Question\",\"duplicateCount\":2,\"errors\":[{\"id\":9}]}";

        QuestionDto dto = objectMapper.readValue(json, QuestionDto.class);

        assertEquals(7L, dto.getId());
        assertEquals(2, dto.getDuplicateCount());
        assertEquals(1, dto.getErrors().size());
    }
}
