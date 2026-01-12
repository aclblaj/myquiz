package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.QuizDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API interface for Quiz operations.
 * This interface defines the contract for quiz management endpoints.
 */
@Tag(name = "Quizzes", description = "Quiz management operations")
public interface QuizApi {

    @Operation(summary = "Get all quizzes", description = "Retrieve all quizzes in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved quizzes"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/quizzes")
    ResponseEntity<List<QuizDto>> getAllQuizzes();

    @Operation(summary = "Get quiz by ID", description = "Retrieve a specific quiz by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved quiz"),
            @ApiResponse(responseCode = "404", description = "Quiz not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/quizzes/{id}")
    ResponseEntity<QuizDto> getQuizById(
            @Parameter(description = "Quiz ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Create new quiz", description = "Create a new quiz")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Quiz created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/quizzes")
    ResponseEntity<QuizDto> createQuiz(
            @Parameter(description = "Quiz data", required = true)
            @RequestBody QuizDto quizDto);

    @Operation(summary = "Update quiz", description = "Update an existing quiz")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quiz updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Quiz not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/api/quizzes/{id}")
    ResponseEntity<QuizDto> updateQuiz(
            @Parameter(description = "Quiz ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated quiz data", required = true)
            @RequestBody QuizDto quizDto);

    @Operation(summary = "Delete quiz", description = "Delete a quiz by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Quiz deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Quiz not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/api/quizzes/{id}")
    ResponseEntity<Void> deleteQuiz(
            @Parameter(description = "Quiz ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Get quizzes by course", description = "Retrieve all quizzes for a specific course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved quizzes"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/quizzes/course/id/{courseId}")
    ResponseEntity<List<QuizDto>> getQuizzesByCourseId(
            @Parameter(description = "Course ID", required = true)
            @PathVariable Long courseId);

    @GetMapping("/api/quizzes/course/name/{courseName}")
    @Operation(summary = "Get quizzes by course name", description = "Retrieve all quizzes for a specific course by its name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved quizzes"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<List<QuizDto>> getQuizzesByCourseName(
            @PathVariable("courseName") String courseName);
    }
