package com.unitbv.myquiz.thy.service;

import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Thin client service for delegating question correction operations to myquiz-app.
 * All AI logic lives in myquiz-app's QuestionCorrectionService.
 * This service handles API communication, error handling, and session management.
 */
@Service
public class QuestionCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionCorrectionService.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public QuestionCorrectionService(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Corrects grammar and spelling in question using AI.
     * @param correctionDto the question correction request
     * @return corrected question data
     * @throws CorrectionServiceException if correction fails or session is invalid
     */
    public QuestionCorrectionDto correctGrammar(QuestionCorrectionDto correctionDto) {
        log.atInfo().log("Delegating grammar correction to myquiz-app");
        return callCorrectionEndpoint(correctionDto, "grammar", QuestionCorrectionDto.class);
    }

    /**
     * Improves question clarity and precision using AI.
     * @param correctionDto the question correction request
     * @return improved question data
     * @throws CorrectionServiceException if improvement fails or session is invalid
     */
    public QuestionCorrectionDto improveQuestion(QuestionCorrectionDto correctionDto) {
        log.atInfo().log("Delegating question improvement to myquiz-app");
        return callCorrectionEndpoint(correctionDto, "improve", QuestionCorrectionDto.class);
    }

    /**
     * Generates alternative answer options using AI.
     * @param correctionDto the question correction request
     * @return generated alternatives as string
     * @throws CorrectionServiceException if generation fails or session is invalid
     */
    public String generateAlternatives(QuestionCorrectionDto correctionDto) {
        log.atInfo().log("Delegating generate alternatives to myquiz-app");
        Map<String, String> response = callCorrectionEndpoint(
            correctionDto, 
            "alternatives", 
            new ParameterizedTypeReference<Map<String, String>>() {}
        );
        return response != null ? response.getOrDefault("alternatives", "") : "";
    }

    /**
     * Generates explanation for correct answer using AI.
     * @param correctionDto the question correction request
     * @return explanation as string
     * @throws CorrectionServiceException if explanation fails or session is invalid
     */
    public String explainAnswer(QuestionCorrectionDto correctionDto) {
        log.atInfo().log("Delegating explain answer to myquiz-app");
        Map<String, String> response = callCorrectionEndpoint(
            correctionDto, 
            "explanation", 
            new ParameterizedTypeReference<Map<String, String>>() {}
        );
        return response != null ? response.getOrDefault("explanation", "") : "";
    }

    /**
     * Generic method to call correction endpoints with consistent error handling.
     * @param correctionDto the correction request data
     * @param operation the operation type (grammar, improve, alternatives, explanation)
     * @param responseType the expected response type
     * @param <T> response type
     * @return the response body
     * @throws CorrectionServiceException if the API call fails
     */
    private <T> T callCorrectionEndpoint(QuestionCorrectionDto correctionDto, 
                                          String operation, 
                                          Class<T> responseType) {
        try {
            Long questionId = extractQuestionId(correctionDto);
            String url = buildCorrectionUrl(questionId, operation);
            
            HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().setMessage("Session expired during {} operation").addArgument(operation).log();
            sessionService.invalidateCurrentSession();
            throw new CorrectionServiceException("Session expired. Please log in again.", ex);
        } catch (HttpClientErrorException ex) {
            log.atError().setMessage("HTTP error during {} operation: {} - {}").addArgument(operation).addArgument(ex.getStatusCode()).addArgument(ex.getMessage()).log();
            throw new CorrectionServiceException(
                String.format("Failed to %s question: %s", operation, ex.getMessage()), 
                ex
            );
        } catch (Exception ex) {
            log.atError().setCause(ex).setMessage("Unexpected error during {} operation").addArgument(operation).log();
            throw new CorrectionServiceException(
                String.format("Unexpected error during %s operation: %s", operation, ex.getMessage()), 
                ex
            );
        }
    }

    /**
     * Overloaded method for ParameterizedTypeReference (for Map responses)
     */
    private <T> T callCorrectionEndpoint(QuestionCorrectionDto correctionDto, 
                                          String operation, 
                                          ParameterizedTypeReference<T> responseType) {
        try {
            Long questionId = extractQuestionId(correctionDto);
            String url = buildCorrectionUrl(questionId, operation);
            
            HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().setMessage("Session expired during {} operation").addArgument(operation).log();
            sessionService.invalidateCurrentSession();
            throw new CorrectionServiceException("Session expired. Please log in again.", ex);
        } catch (HttpClientErrorException ex) {
            log.atError().setMessage("HTTP error during {} operation: {} - {}").addArgument(operation).addArgument(ex.getStatusCode()).addArgument(ex.getMessage()).log();
            throw new CorrectionServiceException(
                String.format("Failed to %s question: %s", operation, ex.getMessage()), 
                ex
            );
        } catch (Exception ex) {
            log.atError().setCause(ex).setMessage("Unexpected error during {} operation").addArgument(operation).log();
            throw new CorrectionServiceException(
                String.format("Unexpected error during %s operation: %s", operation, ex.getMessage()), 
                ex
            );
        }
    }

    /**
     * Extracts question ID from correction DTO.
     */
    private Long extractQuestionId(QuestionCorrectionDto correctionDto) {
        if (correctionDto == null || correctionDto.getOriginalQuestion() == null) {
            throw new IllegalArgumentException("Correction DTO must contain original question with ID");
        }
        return correctionDto.getOriginalQuestion().getId();
    }

    /**
     * Builds the correction API URL.
     */
    private String buildCorrectionUrl(Long questionId, String operation) {
        return String.format("%s%s/%d/correction/%s", 
            apiBaseUrl, 
            ControllerSettings.API_QUESTIONS, 
            questionId, 
            operation
        );
    }

    /**
     * Custom exception for correction service errors.
     */
    public static class CorrectionServiceException extends RuntimeException {
        public CorrectionServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
