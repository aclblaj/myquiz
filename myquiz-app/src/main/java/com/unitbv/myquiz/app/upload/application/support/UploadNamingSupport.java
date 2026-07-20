package com.unitbv.myquiz.app.upload.application.support;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.services.FileService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class UploadNamingSupport {
    private final FileService fileService;

    public UploadNamingSupport(FileService fileService) {
        this.fileService = fileService;
    }

    public String safeArchiveName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unknown.zip";
        }
        return fileService.getFilename(originalName);
    }

    public boolean isZipArchive(MultipartFile archive) {
        String fileName = archive.getOriginalFilename();
        return fileName != null && fileName.toLowerCase().endsWith(".zip");
    }

    public String generateUniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public CourseDto buildAutoCourse(String courseName) {
        CourseDto courseDto = new CourseDto();
        courseDto.setCourse(courseName);
        courseDto.setDescription("Auto-generated for archive folder processing");
        courseDto.setSemester("1");
        courseDto.setUniversityYear("1");
        return courseDto;
    }
}

