package com.unitbv.myquiz.app.upload.application.support;

import com.unitbv.myquiz.api.dto.ArchiveFolderItemDto;
import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import org.springframework.stereotype.Component;

@Component
public class ArchiveFolderResultSupport {
    public static final String MSG_SKIPPED_NON_ARCHIVE = "Skipped non-archive file";
    public static final String MSG_SKIPPED_DUPLICATE_SIZE = "Skipped archive because another processed archive has the same size";

    public ArchiveFolderItemDto createItem(int index, int total, String archiveName) {
        ArchiveFolderItemDto item = new ArchiveFolderItemDto();
        item.setIndex(index);
        item.setTotal(total);
        item.setArchiveName(archiveName);
        return item;
    }

    public void markSkipped(ArchiveFolderUploadResultDto result, ArchiveFolderItemDto item, String message) {
        item.setStatus(ArchiveFolderItemStatus.SKIPPED.value());
        item.setMessage(message);
        result.setSkippedArchives(result.getSkippedArchives() + 1);
    }

    public void markProcessed(ArchiveFolderUploadResultDto result, ArchiveFolderItemDto item, ArchiveUploadResult uploadResult) {
        item.setStatus(ArchiveFolderItemStatus.PROCESSED.value());
        item.setFilesProcessed(uploadResult.filesProcessed());
        item.setMessage(uploadResult.toMessage());
        result.setProcessedArchives(result.getProcessedArchives() + 1);
    }

    public void markFailed(ArchiveFolderUploadResultDto result, ArchiveFolderItemDto item, Exception e) {
        item.setStatus(ArchiveFolderItemStatus.FAILED.value());
        item.setMessage(formatFailureMessage(e));
        result.setFailedArchives(result.getFailedArchives() + 1);
    }

    public String formatFailureMessage(Exception e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}

