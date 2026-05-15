package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorFormDataDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesNewAuthorProperty() throws Exception {
        String json = "{\"author\":{\"id\":1,\"name\":\"Alice Example\",\"initials\":\"AE\"}}";

        AuthorFormDataDto dto = objectMapper.readValue(json, AuthorFormDataDto.class);

        assertNotNull(dto.getAuthor());
        assertEquals(1L, dto.getAuthor().getId());
        assertEquals("Alice Example", dto.getAuthor().getName());
    }

    @Test
    void serializesCanonicalAuthorProperty() throws Exception {
        AuthorFormDataDto dto = new AuthorFormDataDto();
        AuthorDto author = new AuthorDto();
        author.setId(3L);
        author.setName("Carol Example");
        author.setInitials("CE");
        dto.setAuthor(author);

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"author\":"));
        assertTrue(json.contains("\"name\":\"Carol Example\""));
    }
}



