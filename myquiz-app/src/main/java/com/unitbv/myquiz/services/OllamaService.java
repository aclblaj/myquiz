package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquizapi.dto.OllamaResponseDto;

/**
 * Service interface for Ollama AI integration
 */
public interface OllamaService {

    /**
     * Generate AI response using Ollama API
     * @param model the Ollama model to use (e.g., "llama3")
     * @param prompt the prompt to send to the AI
     * @return OllamaResponseDto containing the AI response
     */
    OllamaResponseDto generateResponse(String model, String prompt);

    /**
     * Improve a quiz question using AI
     * @param question the question to improve
     * @return improved question text
     */
    String improveQuestion(Question question);

    /**
     * Generate alternative answers for a multiple choice question
     * @param question the question
     * @param numAlternatives number of alternative answers to generate
     * @return array of alternative answers
     */
    String[] generateAlternativeAnswers(Question question, int numAlternatives);

    /**
     * Correct grammar and spelling in a question
     * @param questionText the question text to correct
     * @param language the language code (e.g., "ro" for Romanian, "en" for English)
     * @return corrected question text
     */
    String correctQuestionText(String questionText, String language);

    /**
     * Generate a detailed explanation for a question's correct answer
     * @param question the question
     * @return explanation text
     */
    String generateExplanation(Question question);

    /**
     * Check agent status
     * @return status of agent
     */
    boolean testConnection();
}
