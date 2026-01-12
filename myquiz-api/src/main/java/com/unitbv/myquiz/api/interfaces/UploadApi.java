package com.unitbv.myquiz.api.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * API interface for Upload operations.
 * This interface defines the contract for file upload endpoints.
 */
@Tag(name = "Upload", description = "File upload operations for quizzes and questions")
public interface UploadApi {

    @Operation(summary = "Upload Excel file", description = "Upload an Excel file containing quiz questions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/api/upload-excel", consumes = "multipart/form-data")
    ResponseEntity<String> uploadExcelFile(
            @Parameter(description = "Excel file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Username of the author", required = true)
            @RequestParam("username") String username,
            @Parameter(description = "Course ID", required = true)
            @RequestParam("courseId") Long courseId,
            @Parameter(description = "Quiz name", required = true)
            @RequestParam("name") String name,
            @Parameter(description = "Template type", required = true)
            @RequestParam("template") String template);

    @Operation(summary = "Upload archive file", description = "Upload an archive file containing multiple quiz files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archive uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid archive format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/api/upload-archive", consumes = "multipart/form-data")
    ResponseEntity<String> uploadArchiveFile(
            @Parameter(description = "Archive file to upload", required = true)
            @RequestParam("archive") MultipartFile archive,
            @Parameter(description = "Course ID", required = true)
            @RequestParam("courseId") Long courseId,
            @Parameter(description = "Quiz name", required = true)
            @RequestParam("quiz") String quizName,
            @Parameter(description = "Year", required = true)
            @RequestParam("year") Long year);
}

