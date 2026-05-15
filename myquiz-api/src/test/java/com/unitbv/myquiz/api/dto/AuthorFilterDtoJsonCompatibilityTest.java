package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorFilterResponseDtoJsonCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesNewAuthorOptionsProperty() throws Exception {
        String json = "{\"authorOptions\":[{\"id\":10,\"name\":\"Alice\"}]}";

        AuthorFilterResponseDto dto = objectMapper.readValue(json, AuthorFilterResponseDto.class);

        assertNotNull(dto.getAuthorOptions());
        assertEquals(1, dto.getAuthorOptions().size());
        assertEquals(10L, dto.getAuthorOptions().getFirst().getId());
        assertEquals("Alice", dto.getAuthorOptions().getFirst().getName());
    }

    @Test
    void deserializesLegacyAuthorListProperty() throws Exception {
        String json = "{\"authorList\":[{\"id\":20,\"name\":\"Bob\"}]}";

        AuthorFilterResponseDto dto = objectMapper.readValue(json, AuthorFilterResponseDto.class);

        assertNotNull(dto.getAuthorOptions());
        assertEquals(1, dto.getAuthorOptions().size());
        assertEquals(20L, dto.getAuthorOptions().getFirst().getId());
        assertEquals("Bob", dto.getAuthorOptions().getFirst().getName());
    }

    @Test
    void serializesBothNewAndLegacyAuthorCollectionPropertyNames() throws Exception {
        AuthorFilterResponseDto dto = new AuthorFilterResponseDto();
        dto.setAuthorOptions(java.util.List.of(new AuthorInfo(30L, "Carol", null)));

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"authorOptions\":"));
        assertTrue(json.contains("\"authorList\":"));
        assertTrue(json.contains("\"name\":\"Carol\""));
    }
}

