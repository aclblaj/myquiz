package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * API interface for Question Bank operations.
 * This interface defines the contract for question bank management endpoints.
 */
@Tag(name = "Question Banks", description = "Question bank management operations")
public interface QuestionBankApi {

    @Operation(summary = "Get all question banks", description = "Retrieve all question banks in the system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved question banks"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping
    ResponseEntity<List<QuestionBankDto>> getAllQuestionBanks();

    @Operation(summary = "Get question bank by ID", description = "Retrieve a specific question bank by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved question bank"), @ApiResponse(responseCode = "404", description = "Question bank not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}")
    ResponseEntity<QuestionBankDto> getQuestionBankById(@Parameter(description = "Question Bank ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get extended question bank export view", description = "Retrieve question bank details grouped by author including questions, errors, and duplicate summaries")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved extended question bank view"), @ApiResponse(responseCode = "404", description = "Question bank not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}/extended")
    ResponseEntity<QuestionBankExportDto> getQuestionBankExtendedById(@Parameter(description = "Question Bank ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create new question bank", description = "Create a new question bank")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Question bank created successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping
    ResponseEntity<QuestionBankDto> createQuestionBank(@Parameter(description = "Question Bank data", required = true) @RequestBody QuestionBankDto questionBankDto);

    @Operation(summary = "Update question bank", description = "Update an existing question bank")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question bank updated successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "404", description = "Question bank not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}")
    ResponseEntity<QuestionBankDto> updateQuestionBank(@Parameter(description = "Question Bank ID", required = true) @PathVariable Long id,
                                                       @Parameter(description = "Updated question bank data", required = true) @RequestBody QuestionBankDto questionBankDto);

    @Operation(summary = "Delete question bank", description = "Delete a question bank by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Question bank deleted successfully"), @ApiResponse(responseCode = "404", description = "Question bank not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteQuestionBank(@Parameter(description = "Question Bank ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get question banks by course", description = "Retrieve all question banks for a specific course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved question banks"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/course/{courseId}")
    ResponseEntity<List<QuestionBankDto>> getQuestionBanksByCourseId(@Parameter(description = "Course ID", required = true) @PathVariable Long courseId);

    @GetMapping("/course/name/{courseName}")
    @Operation(summary = "Get question banks by course name", description = "Retrieve all question banks for a specific course by its name")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved question banks"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    ResponseEntity<List<QuestionBankDto>> getQuestionBanksByCourseName(@Parameter(description = "Course name", required = true) @PathVariable("courseName") String courseName);

    @Operation(summary = "Export question bank questions as XML", description = "Download Moodle-compatible XML for a specific question bank")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question bank XML exported successfully"), @ApiResponse(responseCode = "403", description = "Missing EXPORT_XML permission"), @ApiResponse(responseCode = "404", description = "Question bank not found")})
    @GetMapping("/{id}/export-xml")
    ResponseEntity<byte[]> exportQuestionBankToXml(@Parameter(description = "Question Bank ID", required = true) @PathVariable("id") Long id);
}


