package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.interfaces.UploadApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.services.UploadService;
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

import java.util.Arrays;

/**
 * REST Controller for file upload operations.
 * <p>
 * Handles Excel and Archive (ZIP) uploads following upload-sd.md specifications.
 * This controller focuses on HTTP request/response handling and delegates all
 * business logic to UploadService.
 * <p>
 * Endpoints:
 * - POST /api/upload/excel - Upload single Excel file
 * - POST /api/upload/archive - Upload ZIP archive with multiple Excel files
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Upload", description = "File upload operations for questionBank questions")
public class UploadController implements UploadApi {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Upload and process a single Excel file with questionBank questions.
     * <p>
     * Follows upload-sd.md Section 2.2 specifications.
     *
     * @param file     Excel file (.xlsx)
     * @param username Author name for the questions
     * @param courseId Course ID to associate with questionBank
     * @param name     QuestionBank short name (e.g., "Q1", "Q2")
     * @param template Template type for parsing (e.g., "Template2024", "Template2023")
     * @return Success message or error details
     */
    @Override
    @PostMapping(value = ControllerSettings.API_UPLOAD_EXCEL, consumes = "multipart/form-data")
    @Operation(summary = "Upload Excel file with questionBank questions", description = "Upload and process a single Excel file containing questionBank questions from one author. " + "The file will be parsed according to the specified template type, and questions " + "will be validated for duplicates.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Excel file uploaded and processed successfully"), @ApiResponse(responseCode = "400", description = "Bad request - invalid course ID or file format"), @ApiResponse(responseCode = "500", description = "Internal server error during processing")})
    public ResponseEntity<String> uploadExcelFile(
            @Parameter(description = "Excel file containing questionBank questions", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Author name (username)", required = true) @RequestParam("username") String username,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "QuestionBank short name (e.g., Q1, Q2)", required = true) @RequestParam("name") String name,
            @Parameter(description = "Template type for parsing", required = true) @RequestParam("template") String template
    ) {

        logger.atInfo().addArgument(file.getOriginalFilename()).addArgument(username).addArgument(courseId).log("uploadExcelFile called: file='{}', author='{}', courseId={}");

        try {
            // Validate inputs
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse template type
            TemplateType templateType;
            try {
                // Use fromType() for case-insensitive matching (handles "template2023", "Template2023", etc.)
                templateType = TemplateType.fromType(template);
                if (templateType == null) {
                    // Fallback: try valueOf() for exact enum name match ("Template2023", "Template2024")
                    templateType = TemplateType.valueOf(template);
                }
            }
            catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid template type: " + template + ". Valid values: Template2023, Template2024, template2023, template2024");
            }

            // Delegate to service
            String message = uploadService.processExcelUpload(file, username, courseId, name, templateType);

            logger.atInfo().log("Excel upload completed successfully");
            return ResponseEntity.ok(message);

        }
        catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("Excel upload failed - validation error");
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        }
        catch (Exception e) {
            logger.atError().setCause(e).log("Excel upload failed - unexpected error");
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Upload and process a ZIP archive containing multiple Excel files.
     * <p>
     * Follows upload-sd.md Section 2.4 specifications.
     *
     * @param archive          ZIP archive file containing Excel files
     * @param courseId         Course ID to associate with questionBank
     * @param questionBankName QuestionBank name (e.g., "Midterm Exam")
     * @param studyYear        QuestionBank study year
     * @return Success message with count of imported files or error details
     */
    @Override
    @PostMapping(value = ControllerSettings.API_UPLOAD_ARCHIVE, consumes = "multipart/form-data")
    @Operation(summary = "Upload ZIP archive with multiple Excel files", description = "Upload and process a ZIP archive containing multiple Excel files from different authors. " + "The archive will be extracted, and all Excel files will be processed to create questionBank questions. " + "Author names are extracted from file names or folder structure.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Archive uploaded and processed successfully"), @ApiResponse(responseCode = "400", description = "Bad request - invalid course ID or archive format"), @ApiResponse(responseCode = "500", description = "Internal server error during processing")})
    public ResponseEntity<String> uploadArchiveFile(
            @Parameter(description = "ZIP archive containing Excel files", required = true) @RequestParam("archive") MultipartFile archive,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "Question bank name", required = true) @RequestParam("questionBankName") String questionBankName,
            @Parameter(description = "QuestionBank study year", required = true) @RequestParam("studyYear") StudyYear studyYear
    ) {

        logger.atInfo().addArgument(archive.getOriginalFilename()).addArgument(questionBankName).addArgument(studyYear).log("uploadArchiveFile called: archive='{}', questionBank='{}', studyYear={}");

        try {
            // Validate inputs
            if (archive.isEmpty()) {
                return ResponseEntity.badRequest().body("Archive file is empty");
            }

            if (questionBankName == null || questionBankName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("QuestionBank name is required");
            }

            // Delegate to service
            ArchiveUploadResult result = uploadService.processArchiveUpload(archive, courseId, questionBankName, studyYear);

            logger.atInfo().addArgument(result.filesProcessed()).log("Archive upload completed successfully - {} files processed");

            return ResponseEntity.ok(result.toMessage());

        }
        catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("Archive upload failed - validation error");
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        }
        catch (Exception e) {
            logger.atError().setCause(e).log("Archive upload failed - unexpected error");
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

    }

    @Override
    @PostMapping(value = ControllerSettings.API_UPLOAD_ARCHIVE_FOLDER, consumes = "multipart/form-data")
    @Operation(summary = "Upload a folder of archive files", description = "Process all selected ZIP archives one by one with generated unique course and questionBank names. " + "Files matching previously processed file sizes are skipped.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Folder archives processed"), @ApiResponse(responseCode = "400", description = "No valid archives provided"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<ArchiveFolderUploadResultDto> uploadArchiveFolder(
            @Parameter(description = "ZIP archives selected from a folder", required = true) @RequestParam("archives") MultipartFile[] archives,
            @Parameter(description = "Study year to use for generated questionBanks", required = true) @RequestParam("studyYear") StudyYear studyYear
    ) {
        int providedFiles = archives == null ? 0 : archives.length;
        logger.atInfo().addArgument(providedFiles).addArgument(studyYear).log("uploadArchiveFolder called: files={}, studyYear={}");

        try {
            if (archives == null || archives.length == 0 || Arrays.stream(archives).allMatch(MultipartFile::isEmpty)) {
                logger.atWarn().log("Archive folder upload failed - no files provided");
                return ResponseEntity.badRequest().body(new ArchiveFolderUploadResultDto());
            }
            if (studyYear == null) {
                logger.atWarn().log("Archive folder upload failed - missing studyYear");
                return ResponseEntity.badRequest().body(new ArchiveFolderUploadResultDto());
            }

            ArchiveFolderUploadResultDto result = uploadService.processArchiveFolderUpload(archives, studyYear);
            logger.atInfo().addArgument(result.getTotalArchives()).addArgument(result.getProcessedArchives()).addArgument(result.getSkippedArchives()).addArgument(result.getFailedArchives()).log(
                    "Archive folder upload completed: total={}, processed={}, skipped={}, failed={}");
            return ResponseEntity.ok(result);
        }
        catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("Archive folder upload failed - validation error");
            return ResponseEntity.badRequest().body(new ArchiveFolderUploadResultDto());
        }
        catch (Exception e) {
            logger.atError().setCause(e).log("Archive folder upload failed");
            return ResponseEntity.internalServerError().body(new ArchiveFolderUploadResultDto());
        }
    }

    @Override
    @PostMapping(value = ControllerSettings.API_UPLOAD_XML, consumes = "multipart/form-data")
    @Operation(summary = "Upload XML file with questions", description = "Upload and process Moodle XML generated by MyQuiz export endpoint. " +
            "Questions are imported into a new question bank and skipped when title+text already exist in the selected course.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "XML uploaded and processed successfully"), @ApiResponse(responseCode = "400", description = "Bad request - invalid course ID or XML format"), @ApiResponse(responseCode = "500", description = "Internal server error during processing")})
    public ResponseEntity<String> uploadXmlFile(
            @Parameter(description = "XML file containing questions", required = true) @RequestParam("xml") MultipartFile xml,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "Question bank name", required = true) @RequestParam("questionBankName") String questionBankName,
            @Parameter(description = "QuestionBank study year", required = true) @RequestParam("studyYear") StudyYear studyYear
    ) {
        logger.atInfo().addArgument(xml.getOriginalFilename()).addArgument(questionBankName).addArgument(studyYear)
                .log("uploadXmlFile called: xml='{}', questionBank='{}', studyYear={}");

        try {
            if (xml.isEmpty()) {
                return ResponseEntity.badRequest().body("XML file is empty");
            }
            if (questionBankName == null || questionBankName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("QuestionBank name is required");
            }

            String result = uploadService.processXmlUpload(xml, courseId, questionBankName, studyYear);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log("XML upload failed - validation error");
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.atError().setCause(e).log("XML upload failed - unexpected error");
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
