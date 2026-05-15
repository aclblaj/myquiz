package com.unitbv.myquiz.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated result for folder archive upload processing.
 */
public class ArchiveFolderUploadResultDto {
    private int totalArchives;
    private int processedArchives;
    private int skippedArchives;
    private int failedArchives;
    private List<ArchiveFolderItemDto> items = new ArrayList<>();
    private List<ArchiveImportDto> archiveImports = new ArrayList<>();

    public ArchiveFolderUploadResultDto() {
    }

    public int getTotalArchives() {
        return totalArchives;
    }

    public void setTotalArchives(int totalArchives) {
        this.totalArchives = totalArchives;
    }

    public int getProcessedArchives() {
        return processedArchives;
    }

    public void setProcessedArchives(int processedArchives) {
        this.processedArchives = processedArchives;
    }

    public int getSkippedArchives() {
        return skippedArchives;
    }

    public void setSkippedArchives(int skippedArchives) {
        this.skippedArchives = skippedArchives;
    }

    public int getFailedArchives() {
        return failedArchives;
    }

    public void setFailedArchives(int failedArchives) {
        this.failedArchives = failedArchives;
    }

    public List<ArchiveFolderItemDto> getItems() {
        return items;
    }

    public void setItems(List<ArchiveFolderItemDto> items) {
        this.items = items;
    }

    public List<ArchiveImportDto> getArchiveImports() {
        return archiveImports;
    }

    public void setArchiveImports(List<ArchiveImportDto> archiveImports) {
        this.archiveImports = archiveImports;
    }
}

