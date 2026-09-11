package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.OllamaRequestDto;
import com.unitbv.myquiz.api.dto.OllamaResponseDto;
import com.unitbv.myquiz.api.interfaces.OllamaApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.OllamaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Ollama AI integration endpoints
 */
@RestController
@RequestMapping("/api/ollama")
public class OllamaController implements OllamaApi {

    private static final Logger log = LoggerFactory.getLogger(OllamaController.class);
    private final OllamaService ollamaService;

    // Remove @Autowired for constructor injection (Spring 4.3+ does this automatically)
    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Generate AI response using Ollama
     */
    @Override
    public ResponseEntity<OllamaResponseDto> generateContent(@Valid @RequestBody OllamaRequestDto request) {
        try {
            log.atInfo().addArgument(request.getModel()).addArgument(request.getPrompt().length())
                    .log("Generating AI response with model: {} for prompt length: {}");
            return ResponseEntity.ok(ollamaService.generateResponse(request.getModel(), request.getPrompt()));
        } catch (Exception e) {
            log.atError().setCause(e).log("Error generating AI response");
            return ResponseEntity.status(503).build();
        }
    }

    @Override
    public ResponseEntity<OllamaResponseDto> generateQuestions(
            @RequestParam("topic") String topic,
            @RequestParam(value = "count", defaultValue = "5") Integer count) {
        if (topic == null || topic.isBlank() || count == null || count < 1) {
            return ResponseEntity.badRequest().build();
        }

        String prompt = "Generate " + count + " quiz questions about the following topic: " + topic.trim();
        try {
            return ResponseEntity.ok(ollamaService.generateResponse(null, prompt));
        } catch (Exception e) {
            log.atError().setCause(e).log("Error generating questions for topic");
            return ResponseEntity.status(503).build();
        }
    }

    /**
     * Improve questions using AI
     */
    @Override
    public ResponseEntity<Map<String, Object>> improveQuestions(@NotEmpty @RequestBody List<Long> questionIds) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Question improvement process initiated");
            response.put("questionIds", questionIds);
            response.put("timestamp", OffsetDateTime.now());

            // Implementation would process questions through AI for improvement
            log.atInfo().addArgument(questionIds.size()).log("Processing {} questions for AI improvement");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error improving questions with AI");
            return ResponseEntity.internalServerError().body(Map.of(ControllerSettings.KEY_ERROR, "Question improvement failed", ControllerSettings.RESPONSE_KEY_MESSAGE, e.getMessage()));
        }
    }

    /**
     * Get AI model status and availability
     */
    @Override
    public ResponseEntity<String> checkStatus() {
        try {
            if (ollamaService.testConnection()) {
                return ResponseEntity.ok("operational");
            }
            return ResponseEntity.status(503).body("unavailable");
        } catch (Exception e) {
            log.atError().setCause(e).log("Error checking AI service status");
            return ResponseEntity.status(503).body("unavailable");
        }
    }
}
