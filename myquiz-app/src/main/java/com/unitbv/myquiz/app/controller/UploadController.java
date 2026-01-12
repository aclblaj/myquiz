package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.interfaces.UploadApi;
import com.unitbv.myquiz.app.services.UploadService;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.TemplateType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for file upload operations.
 *
 * Handles Excel and Archive (ZIP) uploads following upload-sd.md specifications.
 * This controller focuses on HTTP request/response handling and delegates all
 * business logic to UploadService.
 *
 * Endpoints:
 * - POST /api/upload/excel - Upload single Excel file
 * - POST /api/upload/archive - Upload ZIP archive with multiple Excel files
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Upload", description = "File upload operations for quiz questions")
public class UploadController implements UploadApi {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Upload and process a single Excel file with quiz questions.
     *
     * Follows upload-sd.md Section 2.2 specifications.
     *
     * @param file Excel file (.xlsx or .xls)
     * @param username Author name for the questions
     * @param courseId Course ID to associate with quiz
     * @param name Quiz short name (e.g., "Q1", "Q2")
     * @param template Template type for parsing (e.g., "Template2024", "Template2023")
     * @return Success message or error details
     */
    @Override
    @PostMapping(ControllerSettings.API_UPLOAD_EXCEL)
    @Operation(
        summary = "Upload Excel file with quiz questions",
        description = "Upload and process a single Excel file containing quiz questions from one author. " +
                     "The file will be parsed according to the specified template type, and questions " +
                     "will be validated for duplicates."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Excel file uploaded and processed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid course ID or file format"),
        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
    })
    public ResponseEntity<String> uploadExcelFile(
            @Parameter(description = "Excel file containing quiz questions", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Author name (username)", required = true)
            @RequestParam("username") String username,
            @Parameter(description = "Course ID", required = true)
            @RequestParam("courseId") Long courseId,
            @Parameter(description = "Quiz short name (e.g., Q1, Q2)", required = true)
            @RequestParam("name") String name,
            @Parameter(description = "Template type for parsing", required = true)
            @RequestParam("template") String template) {

        logger.atInfo()
              .addArgument(file.getOriginalFilename())
              .addArgument(username)
              .addArgument(courseId)
              .log("uploadExcelFile called: file='{}', author='{}', courseId={}");

        try {
            // Validate inputs
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse template type
            TemplateType templateType;
            try {
                templateType = TemplateType.valueOf(template);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid template type: " + template);
            }

            // Delegate to service
            String message = uploadService.processExcelUpload(
                file, username, courseId, name, templateType);

            logger.atInfo().log("Excel upload completed successfully");
            return ResponseEntity.ok(message);

        } catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("Excel upload failed - validation error");
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.atError().setCause(e).log("Excel upload failed - unexpected error");
            return ResponseEntity.internalServerError()
                .body("Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Upload and process a ZIP archive containing multiple Excel files.
     *
     * Follows upload-sd.md Section 2.4 specifications.
     *
     * @param archive ZIP archive file containing Excel files
     * @param courseId Course ID to associate with quiz
     * @param quizName Quiz name (e.g., "Midterm Exam")
     * @param year Quiz year
     * @return Success message with count of imported files or error details
     */
    @Override
    @PostMapping(ControllerSettings.API_UPLOAD_ARCHIVE)
    @Operation(
        summary = "Upload ZIP archive with multiple Excel files",
        description = "Upload and process a ZIP archive containing multiple Excel files from different authors. " +
                     "The archive will be extracted, and all Excel files will be processed to create quiz questions. " +
                     "Author names are extracted from file names or folder structure."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Archive uploaded and processed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid course ID or archive format"),
        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
    })
    public ResponseEntity<String> uploadArchiveFile(
            @Parameter(description = "ZIP archive containing Excel files", required = true)
            @RequestParam("archive") MultipartFile archive,
            @Parameter(description = "Course ID", required = true)
            @RequestParam("courseId") Long courseId,
            @Parameter(description = "Quiz name", required = true)
            @RequestParam("quiz") String quizName,
            @Parameter(description = "Quiz year", required = true)
            @RequestParam("year") Long year) {

        logger.atInfo()
              .addArgument(archive.getOriginalFilename())
              .addArgument(quizName)
              .addArgument(year)
              .log("uploadArchiveFile called: archive='{}', quiz='{}', year={}");

        try {
            // Validate inputs
            if (archive.isEmpty()) {
                return ResponseEntity.badRequest().body("Archive file is empty");
            }

            if (quizName == null || quizName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Quiz name is required");
            }

            // Delegate to service
            UploadService.ArchiveUploadResult result = uploadService.processArchiveUpload(
                archive, courseId, quizName, year);

            logger.atInfo()
                  .addArgument(result.getFilesProcessed())
                  .log("Archive upload completed successfully - {} files processed");

            return ResponseEntity.ok(result.toMessage());

        } catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("Archive upload failed - validation error");
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.atError().setCause(e).log("Archive upload failed - unexpected error");
            return ResponseEntity.internalServerError()
                .body("Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

    }
}
