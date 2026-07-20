package com.unitbv.myquiz.app.upload.api.support;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.types.StudyYear;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Component
public class UploadRequestValidator {

    public String validateNonEmptyFile(MultipartFile file, String emptyMessage) {
        if (file == null || file.isEmpty()) {
            return emptyMessage;
        }
        return null;
    }

    public String validateRequiredText(String value, String emptyMessage) {
        if (value == null || value.trim().isEmpty()) {
            return emptyMessage;
        }
        return null;
    }

    public boolean hasNoArchivesProvided(MultipartFile[] archives) {
        return archives == null || archives.length == 0 || Arrays.stream(archives).allMatch(MultipartFile::isEmpty);
    }

    public boolean isMissingStudyYear(StudyYear studyYear) {
        return studyYear == null;
    }

    public ArchiveFolderUploadResultDto emptyArchiveFolderResult() {
        return new ArchiveFolderUploadResultDto();
    }
}


