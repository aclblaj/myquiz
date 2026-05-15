package com.unitbv.myquiz.api.dto;

import java.time.OffsetDateTime;

/**
 * Metadata for a processed archive file.
 */
public class ArchiveImportDto {
    private Long id;
    private String name;
    private Long size;
    private OffsetDateTime date;

    public ArchiveImportDto() {
    }

    public ArchiveImportDto(Long id, String name, Long size, OffsetDateTime date) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.date = date;
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

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }
}


