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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thymeleaf controller for PDF checking operations using Ollama.
 * Handles PDF file uploads and question answering via AI.
 * Communicates directly with Ollama service.
 */
@Controller
@RequestMapping("/check-pdf")
public class ThyPdfCheckController {
    private static final Logger log = LoggerFactory.getLogger(ThyPdfCheckController.class);
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    @Value("${OLLAMA_API_URL:${ollama.api.url:http://localhost:11434}}")
    private String ollamaApiUrl;
    @Value("${OLLAMA_DEFAULT_MODEL:${ollama.default.model:llama3}}")
    private String defaultModel;

    @Autowired
    public ThyPdfCheckController(SessionService sessionService) {
        this.sessionService = sessionService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();
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

        return ControllerSettings.VIEW_CHECK_PDF;
    }

    /**
     * Check Ollama status
     */
    @GetMapping("/ollama-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkOllamaStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaApiUrl + "/api/tags")).timeout(Duration.ofSeconds(5)).GET().build();

            log.atInfo().addArgument(ollamaApiUrl).log("Checking Ollama status at: {}");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                status.put(ControllerSettings.RESPONSE_KEY_STATUS, ControllerSettings.RESPONSE_KEY_ONLINE);
                status.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Ollama is connected and ready");

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
                log.atInfo().addArgument(modelNames.size()).log("Ollama is online with {} models");
                return ResponseEntity.ok(status);
            } else {
                status.put(ControllerSettings.RESPONSE_KEY_STATUS, "offline");
                status.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Ollama returned status: " + response.statusCode());
                return ResponseEntity.status(503).body(status);
            }
        } catch (Exception e) {
            log.atError().setCause(e).log("Error checking Ollama status");
            status.put(ControllerSettings.RESPONSE_KEY_STATUS, "offline");
            status.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Cannot connect to Ollama: " + e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    /**
     * Process PDF and answer question
     */
    @PostMapping("/ask")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPdfQuestion(@RequestParam("file") MultipartFile file, @RequestParam("question") String question,
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
            log.atInfo().addArgument(pdfText.length()).log("Extracted {} characters from PDF");

            // Build prompt for Ollama
            String prompt = buildPrompt(pdfText, question);

            // Send to Ollama
            String selectedModel = (model != null && !model.isEmpty()) ? model : defaultModel;
            OllamaResponseDto ollamaResponse = sendToOllama(selectedModel, prompt);

            response.put(ControllerSettings.RESPONSE_VALUE_SUCCESS, true);
            response.put("answer", ollamaResponse.getResponse());
            response.put("model", selectedModel);
            response.put("pdfLength", pdfText.length());
            response.put("fileName", file.getOriginalFilename());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.atError().setCause(e).log("Error processing PDF question");
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
                "Based on the following document content, please answer this question:\n\n" + "QUESTION: %s\n\n" + "DOCUMENT CONTENT:\n%s\n\n" + "Please provide a clear and concise answer based only on the information in the document.",
                question, pdfText.substring(0, Math.min(pdfText.length(), 8000)) // Limit to avoid token issues
        );
    }

    /**
     * Send request to Ollama
     */
    private OllamaResponseDto sendToOllama(String model, String prompt) throws IOException, InterruptedException {
        OllamaRequestDto request = OllamaRequestDto.builder().model(model).prompt(prompt).build();
        request.setStream(false); // Disable streaming to get complete response
        String requestJson = objectMapper.writeValueAsString(request);

        log.atInfo().addArgument(model).addArgument(prompt.length()).log("Sending request to Ollama with model: {} (prompt length: {} chars)");

        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(ollamaApiUrl + "/api/generate")).timeout(Duration.ofSeconds(300)) // Increased to 5 minutes for large PDFs
                                             .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestJson)).build();

        log.atDebug().addArgument(ollamaApiUrl).log("Sending request to Ollama at: {}");
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        log.atDebug().addArgument(response.statusCode()).log("Received response from Ollama with status: {}");

        if (response.statusCode() == 200) {
            OllamaResponseDto ollamaResponse = objectMapper.readValue(response.body(), OllamaResponseDto.class);
            log.atInfo().addArgument(ollamaResponse.getResponse() != null ? ollamaResponse.getResponse().length() : 0).log("Successfully received response from Ollama (response length: {} chars)");
            return ollamaResponse;
        } else {
            log.atError().addArgument(response.statusCode()).addArgument(response.body()).log("Ollama API error: {} - {}");
            throw new RuntimeException("Ollama API returned error: " + response.statusCode());
        }
    }
}

