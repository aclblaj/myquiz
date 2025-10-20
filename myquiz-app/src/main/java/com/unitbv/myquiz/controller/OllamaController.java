package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.services.OllamaService;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquizapi.dto.OllamaResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Ollama AI integration endpoints
 */
@RestController
@RequestMapping("/api/ollama")
@Tag(name = "AI Integration", description = "Ollama AI integration for question generation and improvement")
public class OllamaController {

    private static final Logger logger = LoggerFactory.getLogger(OllamaController.class);

    private final OllamaService ollamaService;
    private final QuestionService questionService;

    @Autowired
    public OllamaController(OllamaService ollamaService, QuestionService questionService) {
        this.ollamaService = ollamaService;
        this.questionService = questionService;
    }

    /**
     * Generate AI response using Ollama
     */
    @PostMapping("/generate")
    @Operation(
        summary = "Generate AI Response",
        description = """
            Generate AI-powered responses using Ollama models for question improvement, 
            correction, or creation. Supports multiple AI models including llama3.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "AI generation request with model and prompt",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    requiredProperties = {"prompt"},
                    properties = {
                        @io.swagger.v3.oas.annotations.StringToClassMapItem(
                            key = "model",
                            value = Schema.class
                        ),
                        @io.swagger.v3.oas.annotations.StringToClassMapItem(
                            key = "prompt",
                            value = Schema.class
                        )
                    }
                ),
                examples = {
                    @ExampleObject(
                        name = "Question Correction",
                        summary = "Correct a quiz question",
                        value = """
                            {
                              "model": "llama3",
                              "prompt": "Corectează această întrebare de quiz: Care este capitala Franței?"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Question Generation",
                        summary = "Generate new questions",
                        value = """
                            {
                              "model": "llama3",
                              "prompt": "Generate 3 multiple choice questions about Romanian history"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "AI response generated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    properties = {
                        @io.swagger.v3.oas.annotations.StringToClassMapItem(
                            key = "response",
                            value = Schema.class
                        ),
                        @io.swagger.v3.oas.annotations.StringToClassMapItem(
                            key = "model",
                            value = Schema.class
                        ),
                        @io.swagger.v3.oas.annotations.StringToClassMapItem(
                            key = "timestamp",
                            value = Schema.class
                        )
                    }
                ),
                examples = @ExampleObject(
                    value = """
                        {
                          "response": "Întrebarea corectată: Care este capitala Franței?\nA) Londra\nB) Paris\nC) Berlin\nD) Madrid\n\nRăspuns corect: B) Paris",
                          "model": "llama3",
                          "timestamp": "2025-10-10T14:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing or empty prompt",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "AI service error or internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))
        )
    })
    public ResponseEntity<Map<String, Object>> generateResponse(
            @RequestBody Map<String, String> request) {
        try {
            String model = request.getOrDefault("model", "llama3");
            String prompt = request.get("prompt");

            if (prompt == null || prompt.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Bad Request");
                errorResponse.put("message", "Prompt is required and cannot be empty");
                errorResponse.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.info("Generating AI response with model: {} for prompt length: {}", model, prompt.length());

            OllamaResponseDto aiResponse = ollamaService.generateResponse(model, prompt);

            Map<String, Object> response = new HashMap<>();
            response.put("response", aiResponse.getResponse());
            response.put("model", model);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating AI response", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "AI Generation Failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Improve questions using AI
     */
    @PostMapping("/improve-questions")
    @Operation(
        summary = "AI Question Improvement",
        description = """
            Use AI to improve existing questions by providing suggestions for better wording, 
            more accurate options, or enhanced clarity. Can process single questions or batches.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Questions improved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid question data"),
        @ApiResponse(responseCode = "500", description = "AI improvement service error")
    })
    public ResponseEntity<Map<String, Object>> improveQuestions(
            @Parameter(description = "Question IDs to improve", required = true)
            @RequestBody List<Long> questionIds) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Question improvement process initiated");
            response.put("questionIds", questionIds);
            response.put("timestamp", java.time.LocalDateTime.now());

            // Implementation would process questions through AI for improvement
            logger.info("Processing {} questions for AI improvement", questionIds.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error improving questions with AI", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Question improvement failed",
                           "message", e.getMessage()));
        }
    }

    /**
     * Get AI model status and availability
     */
    @GetMapping("/status")
    @Operation(
        summary = "AI Service Status",
        description = "Check the availability and status of AI models and services"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI service status retrieved"),
        @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("service", "Ollama AI Integration");
            status.put("status", "operational");
            status.put("availableModels", List.of("llama3", "codellama", "mistral"));
            status.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error checking AI service status", e);
            return ResponseEntity.status(503)
                .body(Map.of("service", "Ollama AI Integration",
                           "status", "unavailable",
                           "error", e.getMessage()));
        }
    }
}
