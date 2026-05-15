package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorUpsertDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesNameAndInitials() throws Exception {
        AuthorUpsertDto dto = AuthorUpsertDto.builder()
                .name("Ivan Upsert")
                .initials("IU")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"name\":\"Ivan Upsert\""));
        assertTrue(json.contains("\"initials\":\"IU\""));
    }

    @Test
    void deserializesNameAndInitials() throws Exception {
        String json = "{\"name\":\"Julia Upsert\",\"initials\":\"JU\"}";

        AuthorUpsertDto dto = objectMapper.readValue(json, AuthorUpsertDto.class);

        assertEquals("Julia Upsert", dto.getName());
        assertEquals("JU", dto.getInitials());
    }

    @Test
    void toAuthorDtoWithNullIdProducesCreatePayload() {
        AuthorUpsertDto upsert = AuthorUpsertDto.builder()
                .name("Karl Create")
                .initials("KC")
                .build();

        AuthorDto authorDto = upsert.toAuthorDto(null);

        assertNotNull(authorDto);
        assertNull(authorDto.getId());
        assertEquals("Karl Create", authorDto.getName());
        assertEquals("KC", authorDto.getInitials());
    }

    @Test
    void toAuthorDtoWithIdProducesUpdatePayload() {
        AuthorUpsertDto upsert = AuthorUpsertDto.builder()
                .name("Laura Update")
                .initials("LU")
                .build();

        AuthorDto authorDto = upsert.toAuthorDto(42L);

        assertNotNull(authorDto);
        assertEquals(42L, authorDto.getId());
        assertEquals("Laura Update", authorDto.getName());
        assertEquals("LU", authorDto.getInitials());
    }

    @Test
    void statsFieldsAreAbsentFromSerialization() throws Exception {
        AuthorUpsertDto dto = AuthorUpsertDto.builder()
                .name("Max Stats")
                .initials("MS")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        // Input DTO must NOT leak output-only statistics fields
        assertTrue(!json.contains("numberOfQuestions"), "numberOfQuestions must not appear in input DTO");
        assertTrue(!json.contains("numberOfErrors"), "numberOfErrors must not appear in input DTO");
        assertTrue(!json.contains("numberOfDuplicates"), "numberOfDuplicates must not appear in input DTO");
    }
}

