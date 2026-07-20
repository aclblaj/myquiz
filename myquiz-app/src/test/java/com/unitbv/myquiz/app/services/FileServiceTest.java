package com.unitbv.myquiz.app.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {

    private Path tempDir;

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // Best-effort cleanup for test artifacts.
                            }
                        });
            }
        }
    }

    @Test
    void initFileServiceImpl_usesConfiguredUploadDirectory() throws IOException {
        tempDir = Files.createTempDirectory("file-service-config-");

        FileService service = new FileService();
        ReflectionTestUtils.setField(service, "configuredUploadDir", tempDir.toString());

        service.initFileServiceImpl();

        Path expectedRoot = tempDir.resolve(FileService.UPLOAD_FOLDER);
        assertEquals(expectedRoot.toString(), service.getRootDir());
        assertTrue(Files.isDirectory(expectedRoot));
    }

    @Test
    void uploadFile_stripsEmbeddedDirectoriesFromOriginalFilename() throws IOException {
        tempDir = Files.createTempDirectory("file-service-upload-");

        FileService service = new FileService();
        ReflectionTestUtils.setField(service, "rootDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "nested/folder/report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );

        String savedPath = service.uploadFile(file);

        assertTrue(savedPath.endsWith("_report.xlsx"));
        assertTrue(Files.exists(Path.of(savedPath)));
    }

    @Test
    void removeAllFiles_deletesNestedDirectoriesAndFiles() throws IOException {
        tempDir = Files.createTempDirectory("file-service-cleanup-");
        Path nestedDir = tempDir.resolve("a").resolve("b");
        Files.createDirectories(nestedDir);
        Files.writeString(nestedDir.resolve("test.txt"), "sample");
        Files.writeString(tempDir.resolve("root.txt"), "sample");

        FileService service = new FileService();
        service.removeAllFiles(tempDir.toString());

        assertTrue(Files.isDirectory(tempDir));
        try (Stream<Path> remaining = Files.list(tempDir)) {
            assertFalse(remaining.findFirst().isPresent());
        }
    }
}
