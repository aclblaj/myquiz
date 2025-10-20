package com.unitbv.myquizapi.interfaces;

import com.unitbv.myquizapi.dto.QuestionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API interface for Question operations.
 * This interface defines the contract for question management endpoints.
 */
@Tag(name = "Questions", description = "Question management operations")
public interface QuestionApi {

    @Operation(summary = "Get all questions", description = "Retrieve all questions in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/questions")
    ResponseEntity<List<QuestionDto>> getAllQuestions();

    @Operation(summary = "Get question by ID", description = "Retrieve a specific question by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved question"),
            @ApiResponse(responseCode = "404", description = "Question not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/questions/{id}")
    ResponseEntity<QuestionDto> getQuestionById(
            @Parameter(description = "Question ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Create new question", description = "Create a new question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Question created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/questions")
    ResponseEntity<QuestionDto> createQuestion(
            @Parameter(description = "Question data", required = true)
            @RequestBody QuestionDto questionDto);

    @Operation(summary = "Update question", description = "Update an existing question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Question not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/api/questions/{id}")
    ResponseEntity<QuestionDto> updateQuestion(
            @Parameter(description = "Question ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated question data", required = true)
            @RequestBody QuestionDto questionDto);

    @Operation(summary = "Delete question", description = "Delete a question by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Question deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Question not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/api/questions/{id}")
    ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "Question ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Get questions by quiz", description = "Retrieve all questions for a specific quiz")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
            @ApiResponse(responseCode = "404", description = "Quiz not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/questions/quiz/{quizId}")
    ResponseEntity<List<QuestionDto>> getQuestionsByQuizId(
            @Parameter(description = "Quiz ID", required = true)
            @PathVariable Long quizId);
}
