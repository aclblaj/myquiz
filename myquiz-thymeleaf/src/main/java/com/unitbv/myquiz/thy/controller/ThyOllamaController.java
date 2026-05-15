package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Ollama API endpoints in Thymeleaf frontend
 */
@RestController
@RequestMapping("/api/ollama")
public class ThyOllamaController {

    private static final Logger log = LoggerFactory.getLogger(ThyOllamaController.class);
    private final HttpClient httpClient;
    @Value("${OLLAMA_API_URL:${ollama.api.url:http://localhost:11434}}")
    private String ollamaApiUrl;

    public ThyOllamaController() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Check Ollama service status
     * This endpoint is called by frontend JavaScript
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaApiUrl + "/api/tags")).timeout(Duration.ofSeconds(5)).GET().build();

            log.atDebug().addArgument(ollamaApiUrl).log("Checking Ollama status at: {}");
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                response.put("service", "Ollama AI Integration");
                response.put(ControllerSettings.RESPONSE_KEY_STATUS, "operational");
                response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Ollama is connected and ready");
                log.atDebug().log("Ollama status check successful");
                return ResponseEntity.ok(response);
            } else {
                response.put("service", "Ollama AI Integration");
                response.put(ControllerSettings.RESPONSE_KEY_STATUS, "unavailable");
                response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Ollama returned status: " + httpResponse.statusCode());
                log.atWarn().addArgument(httpResponse.statusCode()).log("Ollama status check failed with status: {}");
                return ResponseEntity.status(503).body(response);
            }
        } catch (Exception e) {
            log.atError().setCause(e).log("Error checking Ollama status");
            response.put("service", "Ollama AI Integration");
            response.put(ControllerSettings.RESPONSE_KEY_STATUS, "unavailable");
            response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Cannot connect to Ollama: " + e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Alternative health check endpoint (for compatibility)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        // Delegate to status endpoint
        ResponseEntity<Map<String, Object>> statusResponse = getStatus();
        Map<String, Object> response = new HashMap<>();

        if (statusResponse.getBody() != null) {
            Map<String, Object> statusBody = statusResponse.getBody();
            boolean isOperational = "operational".equals(statusBody.get(ControllerSettings.RESPONSE_KEY_STATUS));

            response.put("ollama_connected", isOperational);
            response.put(ControllerSettings.RESPONSE_KEY_STATUS, statusBody.get(ControllerSettings.RESPONSE_KEY_STATUS));
            response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, statusBody.get(ControllerSettings.RESPONSE_KEY_MESSAGE));

            return isOperational ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
        }

        response.put("ollama_connected", false);
        response.put(ControllerSettings.RESPONSE_KEY_STATUS, "error");
        response.put(ControllerSettings.RESPONSE_KEY_MESSAGE, "Unable to check Ollama status");
        return ResponseEntity.status(503).body(response);
    }
}

