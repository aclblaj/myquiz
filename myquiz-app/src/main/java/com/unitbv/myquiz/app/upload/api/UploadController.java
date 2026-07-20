package com.unitbv.myquiz.app.upload.api;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.interfaces.UploadApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.upload.application.UploadApplicationService;
import com.unitbv.myquiz.app.upload.api.support.UploadResponseFactory;
import com.unitbv.myquiz.app.upload.api.support.TemplateTypeResolver;
import com.unitbv.myquiz.app.upload.api.support.UploadRequestValidator;
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
 * <p>
 * Handles Excel and Archive (ZIP) uploads following upload-sd.md specifications.
 * This controller focuses on HTTP request/response handling and delegates all
 * business logic to UploadApplicationService.
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
    private static final String MSG_FILE_EMPTY = "File is empty";
    private static final String MSG_ARCHIVE_FILE_EMPTY = "Archive file is empty";
    private static final String MSG_XML_FILE_EMPTY = "XML file is empty";
    private static final String MSG_QUESTION_BANK_REQUIRED = "QuestionBank name is required";
    private static final String LOG_EXCEL_VALIDATION_FAILED = "Excel upload failed - validation error";
    private static final String LOG_EXCEL_UNEXPECTED_FAILED = "Excel upload failed - unexpected error";
    private static final String LOG_ARCHIVE_VALIDATION_FAILED = "Archive upload failed - validation error";
    private static final String LOG_ARCHIVE_UNEXPECTED_FAILED = "Archive upload failed - unexpected error";
    private static final String LOG_XML_VALIDATION_FAILED = "XML upload failed - validation error";
    private static final String LOG_XML_UNEXPECTED_FAILED = "XML upload failed - unexpected error";
    private static final String LOG_ARCHIVE_FOLDER_VALIDATION_FAILED = "Archive folder upload failed - validation error";
    private static final String LOG_ARCHIVE_FOLDER_FAILED = "Archive folder upload failed";
    private static final String LOG_ARCHIVE_FOLDER_NO_FILES = "Archive folder upload failed - no files provided";
    private static final String LOG_ARCHIVE_FOLDER_MISSING_STUDY_YEAR = "Archive folder upload failed - missing studyYear";
    private final UploadApplicationService uploadApplicationService;
    private final TemplateTypeResolver templateTypeResolver;
    private final UploadRequestValidator uploadRequestValidator;
    private final UploadResponseFactory uploadResponseFactory;

    public UploadController(UploadApplicationService uploadApplicationService,
                            TemplateTypeResolver templateTypeResolver,
                            UploadRequestValidator uploadRequestValidator,
                            UploadResponseFactory uploadResponseFactory) {
        this.uploadApplicationService = uploadApplicationService;
        this.templateTypeResolver = templateTypeResolver;
        this.uploadRequestValidator = uploadRequestValidator;
        this.uploadResponseFactory = uploadResponseFactory;
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

        return executeStringUpload(
                LOG_EXCEL_VALIDATION_FAILED,
                LOG_EXCEL_UNEXPECTED_FAILED,
                () -> {
                    ResponseEntity<String> invalidFile = badRequestIfInvalidFile(file, MSG_FILE_EMPTY);
                    if (invalidFile != null) {
                        return invalidFile;
                    }

                    TemplateResolution templateResolution = resolveTemplateOrBadRequest(template);
                    if (templateResolution.badRequest() != null) {
                        return templateResolution.badRequest();
                    }

                    // Delegate to service
                    String message = uploadApplicationService.processExcelUpload(file, username, courseId, name, templateResolution.templateType());

                    logger.atInfo().log("Excel upload completed successfully");
                    return ResponseEntity.ok(message);
                }
        );
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

        return executeStringUpload(
                LOG_ARCHIVE_VALIDATION_FAILED,
                LOG_ARCHIVE_UNEXPECTED_FAILED,
                () -> {
                    ResponseEntity<String> invalidArchive = badRequestIfInvalidFile(archive, MSG_ARCHIVE_FILE_EMPTY);
                    if (invalidArchive != null) {
                        return invalidArchive;
                    }

                    ResponseEntity<String> invalidQuestionBank = badRequestIfInvalidText(questionBankName, MSG_QUESTION_BANK_REQUIRED);
                    if (invalidQuestionBank != null) {
                        return invalidQuestionBank;
                    }

                    // Delegate to service
                    ArchiveUploadResult result = uploadApplicationService.processArchiveUpload(archive, courseId, questionBankName, studyYear);

                    logger.atInfo().addArgument(result.filesProcessed()).log("Archive upload completed successfully - {} files processed");

                    return ResponseEntity.ok(result.toMessage());
                }
        );

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

        ResponseEntity<ArchiveFolderUploadResultDto> invalidRequest = validateArchiveFolderRequest(archives, studyYear);
        if (invalidRequest != null) {
            return invalidRequest;
        }

        return executeArchiveFolderUpload(() -> {
            ArchiveFolderUploadResultDto result = uploadApplicationService.processArchiveFolderUpload(archives, studyYear);
            logger.atInfo().addArgument(result.getTotalArchives()).addArgument(result.getProcessedArchives()).addArgument(result.getSkippedArchives()).addArgument(result.getFailedArchives()).log(
                    "Archive folder upload completed: total={}, processed={}, skipped={}, failed={}");
            return ResponseEntity.ok(result);
        });
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

        return executeStringUpload(
                LOG_XML_VALIDATION_FAILED,
                LOG_XML_UNEXPECTED_FAILED,
                () -> {
                    ResponseEntity<String> invalidXml = badRequestIfInvalidFile(xml, MSG_XML_FILE_EMPTY);
                    if (invalidXml != null) {
                        return invalidXml;
                    }
                    ResponseEntity<String> invalidQuestionBank = badRequestIfInvalidText(questionBankName, MSG_QUESTION_BANK_REQUIRED);
                    if (invalidQuestionBank != null) {
                        return invalidQuestionBank;
                    }

                    String result = uploadApplicationService.processXmlUpload(xml, courseId, questionBankName, studyYear);
                    return ResponseEntity.ok(result);
                }
        );
    }

    private ResponseEntity<String> badRequestIfInvalidFile(MultipartFile file, String message) {
        String validationMessage = uploadRequestValidator.validateNonEmptyFile(file, message);
        return validationMessage != null ? uploadResponseFactory.badRequest(validationMessage) : null;
    }

    private ResponseEntity<String> badRequestIfInvalidText(String value, String message) {
        String validationMessage = uploadRequestValidator.validateRequiredText(value, message);
        return validationMessage != null ? uploadResponseFactory.badRequest(validationMessage) : null;
    }

    private TemplateResolution resolveTemplateOrBadRequest(String template) {
        try {
            return new TemplateResolution(templateTypeResolver.resolve(template), null);
        } catch (IllegalArgumentException e) {
            return new TemplateResolution(null, uploadResponseFactory.badRequest(e.getMessage()));
        }
    }

    private ResponseEntity<String> executeStringUpload(String validationLogMessage,
                                                       String unexpectedLogMessage,
                                                       ThrowingSupplier<ResponseEntity<String>> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log(validationLogMessage);
            return uploadResponseFactory.validationError(e);
        } catch (Exception e) {
            logger.atError().setCause(e).log(unexpectedLogMessage);
            return uploadResponseFactory.internalServerError(e);
        }
    }

    private ResponseEntity<ArchiveFolderUploadResultDto> validateArchiveFolderRequest(MultipartFile[] archives, StudyYear studyYear) {
        if (uploadRequestValidator.hasNoArchivesProvided(archives)) {
            logger.atWarn().log(LOG_ARCHIVE_FOLDER_NO_FILES);
            return uploadResponseFactory.archiveFolderBadRequest(uploadRequestValidator.emptyArchiveFolderResult());
        }
        if (uploadRequestValidator.isMissingStudyYear(studyYear)) {
            logger.atWarn().log(LOG_ARCHIVE_FOLDER_MISSING_STUDY_YEAR);
            return uploadResponseFactory.archiveFolderBadRequest(uploadRequestValidator.emptyArchiveFolderResult());
        }
        return null;
    }

    private ResponseEntity<ArchiveFolderUploadResultDto> executeArchiveFolderUpload(
            ThrowingSupplier<ResponseEntity<ArchiveFolderUploadResultDto>> operation
    ) {
        try {
            return operation.get();
        } catch (IllegalArgumentException e) {
            logger.atWarn().setCause(e).log(LOG_ARCHIVE_FOLDER_VALIDATION_FAILED);
            return uploadResponseFactory.archiveFolderBadRequest();
        } catch (Exception e) {
            logger.atError().setCause(e).log(LOG_ARCHIVE_FOLDER_FAILED);
            return uploadResponseFactory.archiveFolderInternalServerError();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private record TemplateResolution(TemplateType templateType, ResponseEntity<String> badRequest) {
    }
}

