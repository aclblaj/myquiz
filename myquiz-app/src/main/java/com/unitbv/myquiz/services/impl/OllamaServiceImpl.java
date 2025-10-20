package com.unitbv.myquiz.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.services.OllamaService;
import com.unitbv.myquizapi.dto.OllamaRequestDto;
import com.unitbv.myquizapi.dto.OllamaResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service implementation for Ollama AI integration using JSON API calls
 */
@Service
public class OllamaServiceImpl implements OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaServiceImpl.class);

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.default.model:llama3}")
    private String defaultModel;

    @Value("${ollama.timeout.seconds:60}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public OllamaResponseDto generateResponse(String model, String prompt) {
        try {
            // Create Ollama request
            OllamaRequestDto request = new OllamaRequestDto(model != null ? model : defaultModel, prompt);
            String requestJson = objectMapper.writeValueAsString(request);

            logger.info("Sending request to Ollama API: {}", requestJson);

            // Build HTTP request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                OllamaResponseDto ollamaResponse = objectMapper.readValue(response.body(), OllamaResponseDto.class);
                logger.info("Received response from Ollama: {}", ollamaResponse.getResponse());
                return ollamaResponse;
            } else {
                logger.error("Ollama API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Ollama API returned error: " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Ollama API", e);
            throw new RuntimeException("Failed to call Ollama API", e);
        }
    }

    @Override
    public String improveQuestion(Question question) {
        String prompt = String.format(
                "Îmbunătățește această întrebare de quiz pentru a fi mai clară și mai precisă:\n\n" +
                "Întrebare: %s\n" +
                "Text: %s\n\n" +
                "Răspunsuri:\n" +
                "A) %s\n" +
                "B) %s\n" +
                "C) %s\n" +
                "D) %s\n\n" +
                "Te rog să returnezi întrebarea îmbunătățită păstrând același format.",
                question.getTitle(),
                question.getText(),
                question.getResponse1(),
                question.getResponse2(),
                question.getResponse3(),
                question.getResponse4()
        );

        OllamaResponseDto response = generateResponse(defaultModel, prompt);
        return response.getResponse();
    }

    @Override
    public String[] generateAlternativeAnswers(Question question, int numAlternatives) {
        String prompt = String.format(
                "Pentru următoarea întrebare de quiz, generează %d răspunsuri alternative plausibile dar incorecte:\n\n" +
                "Întrebare: %s\n" +
                "Text: %s\n\n" +
                "Răspunsul corect este unul dintre acestea:\n" +
                "- %s\n" +
                "- %s\n" +
                "- %s\n" +
                "- %s\n\n" +
                "Returnează doar răspunsurile alternative, câte unul pe linie, fără numerotare.",
                numAlternatives,
                question.getTitle(),
                question.getText(),
                question.getResponse1(),
                question.getResponse2(),
                question.getResponse3(),
                question.getResponse4()
        );

        OllamaResponseDto response = generateResponse(defaultModel, prompt);
        return response.getResponse().split("\n");
    }

    @Override
    public String correctQuestionText(String questionText, String language) {
        String prompt;
        if ("ro".equals(language)) {
            prompt = String.format(
                    "Corectează gramatica și ortografia acestei întrebări în română:\n\n%s\n\n" +
                    "Returnează doar textul corectat, fără explicații suplimentare.",
                    questionText
            );
        } else {
            prompt = String.format(
                    "Correct the grammar and spelling of this question in English:\n\n%s\n\n" +
                    "Return only the corrected text without additional explanations.",
                    questionText
            );
        }

        OllamaResponseDto response = generateResponse(defaultModel, prompt);
        return response.getResponse().trim();
    }

    @Override
    public String generateExplanation(Question question) {
        String prompt = String.format(
                "Pentru următoarea întrebare de quiz, generează o explicație detaliată pentru răspunsul corect:\n\n" +
                "Întrebare: %s\n" +
                "Text: %s\n\n" +
                "Răspunsuri:\n" +
                "A) %s (punctaj: %.1f)\n" +
                "B) %s (punctaj: %.1f)\n" +
                "C) %s (punctaj: %.1f)\n" +
                "D) %s (punctaj: %.1f)\n\n" +
                "Explică de ce răspunsul cu punctajul cel mai mare este corect și de ce celelalte sunt incorecte.",
                question.getTitle(),
                question.getText(),
                question.getResponse1(), question.getWeightResponse1(),
                question.getResponse2(), question.getWeightResponse2(),
                question.getResponse3(), question.getWeightResponse3(),
                question.getResponse4(), question.getWeightResponse4()
        );

        OllamaResponseDto response = generateResponse(defaultModel, prompt);
        return response.getResponse();
    }

    /**
     * Test connection to Ollama API
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaApiUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            logger.warn("Cannot connect to Ollama API at {}: {}", ollamaApiUrl, e.getMessage());
            return false;
        }
    }
}
