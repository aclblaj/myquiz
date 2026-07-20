package com.unitbv.myquiz.app.upload.api.support;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UploadResponseFactory {

    public ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.badRequest().body(message);
    }

    public ResponseEntity<String> validationError(Exception e) {
        return badRequest("Validation error: " + e.getMessage());
    }

    public ResponseEntity<String> internalServerError(Exception e) {
        return ResponseEntity.internalServerError().body(
                "Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()
        );
    }

    public ResponseEntity<ArchiveFolderUploadResultDto> archiveFolderBadRequest() {
        return ResponseEntity.badRequest().body(new ArchiveFolderUploadResultDto());
    }

    public ResponseEntity<ArchiveFolderUploadResultDto> archiveFolderBadRequest(ArchiveFolderUploadResultDto body) {
        return ResponseEntity.badRequest().body(body);
    }

    public ResponseEntity<ArchiveFolderUploadResultDto> archiveFolderInternalServerError() {
        return ResponseEntity.internalServerError().body(new ArchiveFolderUploadResultDto());
    }
}

