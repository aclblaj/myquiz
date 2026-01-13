package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.OllamaRequestDto;
import com.unitbv.myquiz.api.dto.OllamaResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Thymeleaf controller for PDF checking operations using Ollama.
 * Handles PDF file uploads and question answering via AI.
 * Communicates directly with Ollama service.
 */
@Controller
@RequestMapping("/check-pdf")
public class ThyPdfCheckController {
    private static final Logger log = LoggerFactory.getLogger(ThyPdfCheckController.class);

    @Value("${OLLAMA_API_URL:${ollama.api.url:http://localhost:11434}}")
    private String ollamaApiUrl;

    @Value("${OLLAMA_DEFAULT_MODEL:${ollama.default.model:llama3}}")
    private String defaultModel;

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ThyPdfCheckController(SessionService sessionService) {
        this.sessionService = sessionService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Display the check-pdf page
     */
    @GetMapping({"", "/"})
    public String showCheckPdfPage(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Object loggedInUser = sessionService.getLoggedInUser();
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);

        return "check-pdf";
    }

    /**
     * Check Ollama status
     */
    @GetMapping("/ollama-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkOllamaStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            log.info("Checking Ollama status at: {}", ollamaApiUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                status.put("status", "online");
                status.put("message", "Ollama is connected and ready");

                // Parse available models
                Map<String, Object> tagsResponse = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> models = (List<Map<String, Object>>) tagsResponse.get("models");
                List<String> modelNames = new ArrayList<>();

                if (models != null) {
                    for (Map<String, Object> model : models) {
                        modelNames.add((String) model.get("name"));
                    }
                }

                status.put("models", modelNames);
                log.info("Ollama is online with {} models", modelNames.size());
                return ResponseEntity.ok(status);
            } else {
                status.put("status", "offline");
                status.put("message", "Ollama returned status: " + response.statusCode());
                return ResponseEntity.status(503).body(status);
            }
        } catch (Exception e) {
            log.error("Error checking Ollama status", e);
            status.put("status", "offline");
            status.put("message", "Cannot connect to Ollama: " + e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    /**
     * Process PDF and answer question
     */
    @PostMapping("/ask")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPdfQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam(value = "model", required = false) String model) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("error", "No file uploaded");
                return ResponseEntity.badRequest().body(response);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                response.put("error", "Only PDF files are supported");
                return ResponseEntity.badRequest().body(response);
            }

            // Extract text from PDF
            String pdfText = extractTextFromPdf(file);
            log.info("Extracted {} characters from PDF", pdfText.length());

            // Build prompt for Ollama
            String prompt = buildPrompt(pdfText, question);

            // Send to Ollama
            String selectedModel = (model != null && !model.isEmpty()) ? model : defaultModel;
            OllamaResponseDto ollamaResponse = sendToOllama(selectedModel, prompt);

            response.put("success", true);
            response.put("answer", ollamaResponse.getResponse());
            response.put("model", selectedModel);
            response.put("pdfLength", pdfText.length());
            response.put("fileName", file.getOriginalFilename());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing PDF question", e);
            response.put("error", "Failed to process request: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Extract text from PDF file
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Build prompt for Ollama
     */
    private String buildPrompt(String pdfText, String question) {
        return String.format(
                "Based on the following document content, please answer this question:\n\n" +
                "QUESTION: %s\n\n" +
                "DOCUMENT CONTENT:\n%s\n\n" +
                "Please provide a clear and concise answer based only on the information in the document.",
                question,
                pdfText.substring(0, Math.min(pdfText.length(), 8000)) // Limit to avoid token issues
        );
    }

    /**
     * Send request to Ollama
     */
    private OllamaResponseDto sendToOllama(String model, String prompt) throws IOException, InterruptedException {
        OllamaRequestDto request = OllamaRequestDto
                .builder().model(model).prompt(prompt).build();
        request.setStream(false); // Disable streaming to get complete response
        String requestJson = objectMapper.writeValueAsString(request);

        log.info("Sending request to Ollama with model: {} (prompt length: {} chars)", model, prompt.length());

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(ollamaApiUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(300)) // Increased to 5 minutes for large PDFs
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        log.debug("Sending request to Ollama at: {}", ollamaApiUrl);
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        log.debug("Received response from Ollama with status: {}", response.statusCode());

        if (response.statusCode() == 200) {
            OllamaResponseDto ollamaResponse = objectMapper.readValue(response.body(), OllamaResponseDto.class);
            log.info("Successfully received response from Ollama (response length: {} chars)",
                    ollamaResponse.getResponse() != null ? ollamaResponse.getResponse().length() : 0);
            return ollamaResponse;
        } else {
            log.error("Ollama API error: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Ollama API returned error: " + response.statusCode());
        }
    }
}

