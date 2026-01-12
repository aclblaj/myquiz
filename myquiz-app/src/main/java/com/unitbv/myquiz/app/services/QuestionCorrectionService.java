package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.OllamaRequestDto;
import com.unitbv.myquiz.api.dto.OllamaResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Service for handling question correction operations using AI.
 * This is the backend (myquiz-app) owner of correction functionality.
 */
@Service
public class QuestionCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionCorrectionService.class);

    @Value("${OLLAMA_API_URL:${ollama.api.url:http://localhost:11434}}")
    private String ollamaApiUrl;

    @Value("${OLLAMA_DEFAULT_MODEL:${ollama.default.model:llama3}}")
    private String defaultModel;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QuestionCorrectionService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    private String getModelFromDto(QuestionCorrectionDto correctionDto) {
        if (correctionDto.getModelUsed() != null && !correctionDto.getModelUsed().isBlank()) {
            return correctionDto.getModelUsed();
        }
        return defaultModel;
    }

    /**
     * Correct grammar in the question using AI
     */
    public QuestionCorrectionDto correctGrammar(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Correcting grammar for question ID: {}", correctionDto.getOriginalQuestion().getId());
        String language = correctionDto.getLanguage() != null ? correctionDto.getLanguage() : "ro";
        String model = getModelFromDto(correctionDto);
        correctionDto.copyOriginalToModified();
        QuestionDto modified = correctionDto.getModifiedQuestion();

        // Correct title if present
        if (modified.getTitle() != null && !modified.getTitle().isEmpty()) {
            String correctedTitle = correctText(modified.getTitle(), language, model);
            modified.setTitle(correctedTitle);
            log.debug("Corrected title: {} -> {}", correctionDto.getOriginalQuestion().getTitle(), correctedTitle);
        }

        // Correct text if present
        if (modified.getText() != null && !modified.getText().isEmpty()) {
            String correctedText = correctText(modified.getText(), language, model);
            modified.setText(correctedText);
            log.debug("Corrected text: {} -> {}", correctionDto.getOriginalQuestion().getText(), correctedText);
        }

        // Correct response 1
        if (modified.getResponse1() != null && !modified.getResponse1().isEmpty()) {
            String correctedResponse = correctText(modified.getResponse1(), language, model);
            modified.setResponse1(correctedResponse);
            log.debug("Corrected response1");
        }

        // Correct response 2
        if (modified.getResponse2() != null && !modified.getResponse2().isEmpty()) {
            String correctedResponse = correctText(modified.getResponse2(), language, model);
            modified.setResponse2(correctedResponse);
            log.debug("Corrected response2");
        }

        // Correct response 3
        if (modified.getResponse3() != null && !modified.getResponse3().isEmpty()) {
            String correctedResponse = correctText(modified.getResponse3(), language, model);
            modified.setResponse3(correctedResponse);
            log.debug("Corrected response3");
        }

        // Correct response 4
        if (modified.getResponse4() != null && !modified.getResponse4().isEmpty()) {
            String correctedResponse = correctText(modified.getResponse4(), language, model);
            modified.setResponse4(correctedResponse);
            log.debug("Corrected response4");
        }

        correctionDto.setModifiedQuestion(modified);
        correctionDto.setCorrectionType("grammar");
        correctionDto.setModelUsed(model);
        correctionDto.setCorrectionNotes("Grammar and spelling corrected for title, text, and all response options");

        return correctionDto;
    }

    /**
     * Improve the question using AI
     */
    public QuestionCorrectionDto improveQuestion(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Improving question ID: {}", correctionDto.getOriginalQuestion().getId());
        String language = correctionDto.getLanguage() != null ? correctionDto.getLanguage() : "ro";
        String model = getModelFromDto(correctionDto);
        correctionDto.copyOriginalToModified();
        QuestionDto modified = correctionDto.getModifiedQuestion();

        // Improve title if present
        if (modified.getTitle() != null && !modified.getTitle().isEmpty()) {
            String improvedTitle = improveText(modified.getTitle(), "title", language, model);
            modified.setTitle(improvedTitle);
            log.debug("Improved title: {} -> {}", correctionDto.getOriginalQuestion().getTitle(), improvedTitle);
        }

        // Improve text if present
        if (modified.getText() != null && !modified.getText().isEmpty()) {
            String improvedText = improveText(modified.getText(), "question", language, model);
            modified.setText(improvedText);
            log.debug("Improved text: {} -> {}", correctionDto.getOriginalQuestion().getText(), improvedText);
        }

        // Improve response 1
        if (modified.getResponse1() != null && !modified.getResponse1().isEmpty()) {
            String improvedResponse = improveText(modified.getResponse1(), "answer", language, model);
            modified.setResponse1(improvedResponse);
            log.debug("Improved response1");
        }

        // Improve response 2
        if (modified.getResponse2() != null && !modified.getResponse2().isEmpty()) {
            String improvedResponse = improveText(modified.getResponse2(), "answer", language, model);
            modified.setResponse2(improvedResponse);
            log.debug("Improved response2");
        }

        // Improve response 3
        if (modified.getResponse3() != null && !modified.getResponse3().isEmpty()) {
            String improvedResponse = improveText(modified.getResponse3(), "answer", language, model);
            modified.setResponse3(improvedResponse);
            log.debug("Improved response3");
        }

        // Improve response 4
        if (modified.getResponse4() != null && !modified.getResponse4().isEmpty()) {
            String improvedResponse = improveText(modified.getResponse4(), "answer", language, model);
            modified.setResponse4(improvedResponse);
            log.debug("Improved response4");
        }

        correctionDto.setModifiedQuestion(modified);
        correctionDto.setCorrectionType("improve");
        correctionDto.setModelUsed(model);
        correctionDto.setCorrectionNotes("Question improved for clarity and precision in title, text, and all response options");

        return correctionDto;
    }

    /**
     * Generate alternative answers using AI
     */
    public String generateAlternatives(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Generating alternatives for question ID: {}", correctionDto.getOriginalQuestion().getId());
        String model = getModelFromDto(correctionDto);
        QuestionDto original = correctionDto.getOriginalQuestion();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("For this quiz question, generate 4 alternative plausible but incorrect answers:\n\n");
        promptBuilder.append("Question: ").append(original.getTitle()).append("\n");
        promptBuilder.append("Text: ").append(original.getText()).append("\n\n");

        if (original.getResponse1() != null) {
            promptBuilder.append("Current answer options:\n");
            promptBuilder.append("A) ").append(original.getResponse1()).append("\n");
            if (original.getResponse2() != null)
                promptBuilder.append("B) ").append(original.getResponse2()).append("\n");
            if (original.getResponse3() != null)
                promptBuilder.append("C) ").append(original.getResponse3()).append("\n");
            if (original.getResponse4() != null)
                promptBuilder.append("D) ").append(original.getResponse4()).append("\n");
        }

        promptBuilder.append("\nGenerate 4 new alternative answers that are plausible distractors but clearly incorrect. Return only the alternatives, one per line.");

        return generateWithOllama(promptBuilder.toString(), model);
    }

    /**
     * Generate explanation for the correct answer
     */
    public String explainAnswer(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Explaining answer for question ID: {}", correctionDto.getOriginalQuestion().getId());
        String model = getModelFromDto(correctionDto);
        QuestionDto original = correctionDto.getOriginalQuestion();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("For this quiz question, provide a detailed explanation of which answer is correct and why the others are incorrect:\n\n");
        promptBuilder.append("Question: ").append(original.getTitle()).append("\n");
        promptBuilder.append("Text: ").append(original.getText()).append("\n\n");

        promptBuilder.append("Answer Options:\n");
        promptBuilder.append("A) ").append(original.getResponse1());
        promptBuilder.append(" (weight: ").append(original.getWeightResponse1()).append(")\n");
        if (original.getResponse2() != null) {
            promptBuilder.append("B) ").append(original.getResponse2());
            promptBuilder.append(" (weight: ").append(original.getWeightResponse2()).append(")\n");
        }
        if (original.getResponse3() != null) {
            promptBuilder.append("C) ").append(original.getResponse3());
            promptBuilder.append(" (weight: ").append(original.getWeightResponse3()).append(")\n");
        }
        if (original.getResponse4() != null) {
            promptBuilder.append("D) ").append(original.getResponse4());
            promptBuilder.append(" (weight: ").append(original.getWeightResponse4()).append(")\n");
        }

        promptBuilder.append("\nProvide a clear, educational explanation that helps understand the correct answer and common misconceptions.");

        return generateWithOllama(promptBuilder.toString(), model);
    }

    /**
     * Correct text using Ollama API
     */
    private String correctText(String text, String language, String model) throws IOException, InterruptedException {
        String prompt = buildCorrectionPrompt(text, language);
        String response = generateWithOllama(prompt, model);

        // Extract the corrected text from response
        // The AI typically returns just the corrected text
        return response.trim();
    }

    /**
     * Improve text using Ollama API
     */
    private String improveText(String text, String textType, String language, String model) throws IOException, InterruptedException {
        String prompt = buildImprovementPrompt(text, textType, language);
        String response = generateWithOllama(prompt, model);

        // Extract the improved text from response
        return response.trim();
    }

    /**
     * Build correction prompt based on language
     */
    private String buildCorrectionPrompt(String text, String language) {
        if ("en".equals(language)) {
            return "Correct the grammar and spelling of this text in English. Return ONLY the corrected text, nothing else:\n\n" + text;
        } else {
            return "Corectează gramatica și ortografia acestui text în română. Returnează DOAR textul corectat, nimic altceva:\n\n" + text;
        }
    }

    /**
     * Build improvement prompt based on text type and language
     */
    private String buildImprovementPrompt(String text, String textType, String language) {
        if ("en".equals(language)) {
            switch (textType) {
                case "title":
                    return "Improve this quiz question title to be clearer and more concise. Keep it professional and educational. Return ONLY the improved title, nothing else:\n\n" + text;
                case "question":
                    return "Improve this quiz question text to be clearer, more precise, and pedagogically sound. Maintain the same meaning but enhance clarity. Return ONLY the improved question text, nothing else:\n\n" + text;
                case "answer":
                    return "Improve this answer option to be clearer and more precise. Keep it concise. Return ONLY the improved answer, nothing else:\n\n" + text;
                default:
                    return "Improve this text to be clearer and more precise. Return ONLY the improved text, nothing else:\n\n" + text;
            }
        } else {
            switch (textType) {
                case "title":
                    return "Îmbunătățește acest titlu de întrebare pentru a fi mai clar și mai concis. Păstrează-l profesional și educațional. Returnează DOAR titlul îmbunătățit, nimic altceva:\n\n" + text;
                case "question":
                    return "Îmbunătățește textul acestei întrebări pentru a fi mai clar, mai precis și mai corect din punct de vedere pedagogic. Păstrează același înțeles dar îmbunătățește claritatea. Returnează DOAR textul îmbunătățit al întrebării, nimic altceva:\n\n" + text;
                case "answer":
                    return "Îmbunătățește această opțiune de răspuns pentru a fi mai clară și mai precisă. Păstrează-o concisă. Returnează DOAR răspunsul îmbunătățit, nimic altceva:\n\n" + text;
                default:
                    return "Îmbunătățește acest text pentru a fi mai clar și mai precis. Returnează DOAR textul îmbunătățit, nimic altceva:\n\n" + text;
            }
        }
    }

    /**
     * Generate response using Ollama API
     */
    private String generateWithOllama(String prompt, String model) throws IOException, InterruptedException {
        OllamaRequestDto request = new OllamaRequestDto(model, prompt);
        request.setStream(false);
        String requestJson = objectMapper.writeValueAsString(request);

        log.debug("Sending request to Ollama at: {} with model {}", ollamaApiUrl, model);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(ollamaApiUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(300))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            OllamaResponseDto ollamaResponse = objectMapper.readValue(response.body(), OllamaResponseDto.class);
            log.debug("Received response from Ollama");
            return ollamaResponse.getResponse();
        } else {
            log.error("Ollama API error: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Ollama API returned error: " + response.statusCode());
        }
    }
}
