package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * Metadata for a processed archive file.
 */
@Schema(description = "Metadata for a processed archive file")
public class ArchiveImportDto {
    private Long id;
    private String name;
    private Long size;
    @JsonAlias("date")
    private OffsetDateTime processedAt;

    public ArchiveImportDto() {
    }

    public ArchiveImportDto(Long id, String name, Long size, OffsetDateTime processedAt) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}


