package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquizapi.dto.QuizDto;
import com.unitbv.myquizapi.interfaces.QuizApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Quiz Management
 * Provides endpoints for CRUD operations on quizzes
 */
@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "Quiz management operations - Organize questions into cohesive quizzes")
public class QuizApiController implements QuizApi {

    private static final Logger logger = LoggerFactory.getLogger(QuizApiController.class);

    private final QuizService quizService;

    @Autowired
    public QuizApiController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * Get all quizzes
     */
    @GetMapping("")
    @Operation(
        summary = "Get All Quizzes",
        description = "Retrieve a list of all available quizzes with their basic information and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<List<QuizDto>> getAllQuizzes() {
        try {
            List<QuizDto> quizzes = quizService.getAllQuizzes();
            logger.info("Retrieved {} quizzes", quizzes.size());
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            logger.error("Error retrieving all quizzes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get quiz by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Quiz by ID",
        description = "Retrieve detailed information about a specific quiz including questions and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quiz found",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> getQuizById(
            @Parameter(description = "Unique identifier of the quiz", required = true, example = "1")
            @PathVariable Long id) {
        try {
            QuizDto quiz = quizService.getQuizById(id);
            if (quiz != null) {
                logger.info("Retrieved quiz: {}", quiz.getName());
                return ResponseEntity.ok(quiz);
            } else {
                logger.warn("Quiz not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving quiz with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new quiz
     */
    @PostMapping
    @Operation(
        summary = "Create New Quiz",
        description = "Create a new quiz with the specified course, name, and year"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Quiz created successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quiz data",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> createQuiz(@RequestBody QuizDto quizDto) {
        try {
            var quiz = quizService.createQuizz(quizDto.getCourse(), quizDto.getName(), quizDto.getYear());
            QuizDto createdDto = new QuizDto();
            createdDto.setId(quiz.getId());
            createdDto.setName(quiz.getName());
            createdDto.setCourse(quiz.getCourse());
            createdDto.setYear(quiz.getYear());
            return ResponseEntity.status(201).body(createdDto);
        } catch (Exception e) {
            logger.error("Error creating quiz", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update quiz by ID
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update Quiz",
        description = "Update an existing quiz's details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quiz updated successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quiz data",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> updateQuiz(Long id, @RequestBody QuizDto quizDto) {
        try {
            var updatedQuiz = quizService.updateQuiz(id, quizDto.getCourse(), quizDto.getName(), quizDto.getYear());
            if (updatedQuiz == null) return ResponseEntity.notFound().build();
            QuizDto updatedDto = new QuizDto();
            updatedDto.setId(updatedQuiz.getId());
            updatedDto.setName(updatedQuiz.getName());
            updatedDto.setCourse(updatedQuiz.getCourse());
            updatedDto.setYear(updatedQuiz.getYear());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            logger.error("Error updating quiz", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete quiz by ID
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Quiz",
        description = "Permanently delete a quiz and all its associated questions. This operation cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quiz deleted successfully",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/SuccessResponse"))),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<Void> deleteQuiz(Long id) {
        try {
            quizService.deleteQuizById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting quiz with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get quizzes by Course ID
     */
    @GetMapping("/course/{courseId}")
    @Operation(
        summary = "Get Quizzes by Course ID",
        description = "Retrieve a list of quizzes associated with a specific course"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "404", description = "Course not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })

    @Override
    public ResponseEntity<List<QuizDto>> getQuizzesByCourseId(Long courseId) {
        try {
            List<QuizDto> quizzes = quizService.getQuizzesByCourseId(courseId);
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            logger.error("Error retrieving quizzes for course ID: {}", courseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
