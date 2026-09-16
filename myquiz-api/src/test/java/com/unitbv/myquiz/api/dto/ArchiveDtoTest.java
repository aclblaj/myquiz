package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void folderUploadResultKeepsListsNonNull() {
        ArchiveFolderUploadResultDto result = new ArchiveFolderUploadResultDto();

        result.setItems(null);
        result.setArchiveImports(null);

        assertNotNull(result.getItems());
        assertNotNull(result.getArchiveImports());
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getArchiveImports().isEmpty());
    }

    @Test
    void archiveItemStatusUsesStableJsonValues() throws Exception {
        ArchiveFolderItemDto item = new ArchiveFolderItemDto();
        item.setStatus(ArchiveFolderItemStatus.PROCESSED);

        String json = objectMapper.writeValueAsString(item);

        assertTrue(json.contains("\"status\":\"PROCESSED\""));
        ArchiveFolderItemDto restored = objectMapper.readValue(json, ArchiveFolderItemDto.class);
        assertEquals(ArchiveFolderItemStatus.PROCESSED, restored.getStatus());
    }

    @Test
    void archiveImportUsesProcessedAtAndReadsLegacyDate() throws Exception {
        ArchiveImportDto imported = objectMapper.readValue(
                "{\"id\":1,\"name\":\"archive.zip\",\"size\":42,\"date\":\"2026-09-14T12:00:00Z\"}",
                ArchiveImportDto.class);

        assertEquals(OffsetDateTime.parse("2026-09-14T12:00:00Z"), imported.getProcessedAt());

        String json = objectMapper.writeValueAsString(imported);
        assertTrue(json.contains("\"processedAt\""));
        assertFalse(json.contains("\"date\""));
    }
}
