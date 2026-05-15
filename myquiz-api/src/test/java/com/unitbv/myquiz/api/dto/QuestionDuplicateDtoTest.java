package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitbv.myquiz.api.types.QuestionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDuplicateDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesTypeAndResponsesForMultichoiceDuplicates() throws Exception {
        QuestionDuplicateDto dto = new QuestionDuplicateDto();
        dto.setDuplicateLinkId(99L);
        dto.setQuestionId(123L);
        dto.setTitle("Duplicate title");
        dto.setText("Duplicate text");
        dto.setResponse1("A1");
        dto.setResponse2("A2");
        dto.setResponse3("A3");
        dto.setResponse4("A4");
        dto.setType(QuestionType.MULTICHOICE);
        dto.setCourse("RC");
        dto.setQuestionBankName("Quiz 1");
        dto.setAuthorName("Author 1");
        dto.setRow(7);

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"type\":\"MULTICHOICE\""));
        assertTrue(json.contains("\"response1\":\"A1\""));
        assertTrue(json.contains("\"response2\":\"A2\""));
        assertTrue(json.contains("\"response3\":\"A3\""));
        assertTrue(json.contains("\"response4\":\"A4\""));
    }

    @Test
    void gettersReturnExpectedTypeAndResponses() {
        QuestionDuplicateDto dto = new QuestionDuplicateDto();
        dto.setType(QuestionType.TRUEFALSE);
        dto.setResponse1("TRUE");

        assertEquals(QuestionType.TRUEFALSE, dto.getType());
        assertEquals("TRUE", dto.getResponse1());
    }
}

