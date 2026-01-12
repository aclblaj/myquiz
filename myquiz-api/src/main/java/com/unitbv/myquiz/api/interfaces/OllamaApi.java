package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.OllamaRequestDto;
import com.unitbv.myquiz.api.dto.OllamaResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API interface for Ollama AI operations.
 * This interface defines the contract for AI-powered question generation and assistance.
 */
@Tag(name = "AI Assistant", description = "AI-powered operations using Ollama")
public interface OllamaApi {

    @Operation(summary = "Generate content with Ollama", description = "Send a prompt to Ollama AI for content generation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully generated content"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "503", description = "AI service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/ollama/generate")
    ResponseEntity<OllamaResponseDto> generateContent(
            @Parameter(description = "Ollama request data", required = true)
            @RequestBody OllamaRequestDto request);

    @Operation(summary = "Generate quiz questions", description = "Generate quiz questions using AI based on a topic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully generated questions"),
            @ApiResponse(responseCode = "400", description = "Invalid topic"),
            @ApiResponse(responseCode = "503", description = "AI service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/ollama/questions")
    ResponseEntity<OllamaResponseDto> generateQuestions(
            @Parameter(description = "Topic for question generation", required = true)
            @RequestParam String topic,
            @Parameter(description = "Number of questions to generate")
            @RequestParam(defaultValue = "5") Integer count);

    @Operation(summary = "Check Ollama service status", description = "Check if Ollama AI service is available")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is available"),
            @ApiResponse(responseCode = "503", description = "Service unavailable")
    })
    @GetMapping("/api/ollama/status")
    ResponseEntity<String> checkStatus();
}
