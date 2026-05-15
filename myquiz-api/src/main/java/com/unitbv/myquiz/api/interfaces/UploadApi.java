package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.StudyYear;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * API interface for Upload operations.
 * This interface defines the contract for file upload endpoints.
 */
@Tag(name = "Upload", description = "File upload operations for questionBanks and questions")
public interface UploadApi {

    @Operation(summary = "Upload Excel file", description = "Upload an Excel file containing questionBank questions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "File uploaded successfully"), @ApiResponse(responseCode = "400", description = "Invalid file format"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(value = ControllerSettings.API_UPLOAD_EXCEL, consumes = "multipart/form-data")
    ResponseEntity<String> uploadExcelFile(
            @Parameter(description = "Excel file to upload", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Username of the author", required = true) @RequestParam("username") String username,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "questionBank name", required = true) @RequestParam("name") String name,
            @Parameter(description = "Template type", required = true) @RequestParam("template") String template
    );

    @Operation(summary = "Upload archive file", description = "Upload an archive file containing multiple questionBank files")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Archive uploaded successfully"), @ApiResponse(responseCode = "400", description = "Invalid archive format"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(value = ControllerSettings.API_UPLOAD_ARCHIVE, consumes = "multipart/form-data")
    ResponseEntity<String> uploadArchiveFile(
            @Parameter(description = "Archive file to upload", required = true) @RequestParam("archive") MultipartFile archive,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "Question bank name", required = true) @RequestParam("questionBank") String questionBankName,
            @Parameter(description = "Study year", required = true) @RequestParam("studyYear") StudyYear studyYear
    );

    @Operation(summary = "Upload archive folder", description = "Upload and process all ZIP archives selected from a folder")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Folder archives processed successfully"), @ApiResponse(responseCode = "400", description = "Invalid archive list"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(value = ControllerSettings.API_UPLOAD_ARCHIVE_FOLDER, consumes = "multipart/form-data")
    ResponseEntity<ArchiveFolderUploadResultDto> uploadArchiveFolder(
            @Parameter(description = "Archive files selected from a folder", required = true) @RequestParam("archives") MultipartFile[] archives,
            @Parameter(description = "Target study year for generated questionBanks", required = true) @RequestParam("studyYear") StudyYear studyYear
    );

    @Operation(summary = "Upload XML file", description = "Upload and import Moodle XML questions exported from MyQuiz")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "XML uploaded successfully"), @ApiResponse(responseCode = "400", description = "Invalid XML payload"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(value = ControllerSettings.API_UPLOAD_XML, consumes = "multipart/form-data")
    ResponseEntity<String> uploadXmlFile(
            @Parameter(description = "XML file to upload", required = true) @RequestParam("xml") MultipartFile xml,
            @Parameter(description = "Course ID", required = true) @RequestParam("courseId") Long courseId,
            @Parameter(description = "Question bank name", required = true) @RequestParam("questionBankName") String questionBankName,
            @Parameter(description = "Study year", required = true) @RequestParam("studyYear") StudyYear studyYear
    );
}

