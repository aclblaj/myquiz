package com.unitbv.myquiz.app.upload.application.support;

import com.unitbv.myquiz.app.services.FileService;
import com.unitbv.myquiz.app.util.FileValidator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipException;

@Component
public class ArchiveProcessingSupport {
    private final FileService fileService;

    public ArchiveProcessingSupport(FileService fileService) {
        this.fileService = fileService;
    }

    public Path createWritableTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        if (!Files.isWritable(tempDir)) {
            throw new IOException("Temp directory is not writable: " + tempDir);
        }
        return tempDir;
    }

    public void ensureUploadedArchiveExists(Path archiveFilePath) throws IOException {
        if (!FileValidator.exists(archiveFilePath)) {
            throw new IOException("Archive file was not saved to disk: " + archiveFilePath);
        }
    }

    public void extractArchiveContents(Path archiveFilePath, Path tempDir) throws IOException {
        try {
            fileService.unzipAndRenameExcelFiles(archiveFilePath, tempDir);
        } catch (ZipException e) {
            throw new IllegalArgumentException("Invalid or unsupported ZIP archive: " + e.getMessage(), e);
        }
    }

    public void cleanup(Path tempDir, String archivePath) {
        if (tempDir != null) {
            fileService.removeDir(tempDir.toString());
        }
        if (archivePath != null) {
            fileService.removeFile(archivePath);
        }
    }
}

