package com.unitbv.myquiz.thy.service;

import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Thin client service for delegating question correction operations to myquiz-app.
 * All AI logic lives in myquiz-app's QuestionCorrectionService.
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

    public QuestionCorrectionDto correctGrammar(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Delegating grammar correction to myquiz-app");
        Long questionId = correctionDto.getOriginalQuestion() != null ? correctionDto.getOriginalQuestion().getId() : 0L;
        HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
        ResponseEntity<QuestionCorrectionDto> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId + "/correction/grammar",
                HttpMethod.POST,
                entity,
                QuestionCorrectionDto.class
        );
        return response.getBody();
    }

    public QuestionCorrectionDto improveQuestion(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Delegating question improvement to myquiz-app");
        Long questionId = correctionDto.getOriginalQuestion() != null ? correctionDto.getOriginalQuestion().getId() : 0L;
        HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
        ResponseEntity<QuestionCorrectionDto> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId + "/correction/improve",
                HttpMethod.POST,
                entity,
                QuestionCorrectionDto.class
        );
        return response.getBody();
    }

    public String generateAlternatives(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Delegating generate alternatives to myquiz-app");
        Long questionId = correctionDto.getOriginalQuestion() != null ? correctionDto.getOriginalQuestion().getId() : 0L;
        HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
        ResponseEntity<java.util.Map> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId + "/correction/alternatives",
                HttpMethod.POST,
                entity,
                java.util.Map.class
        );
        Object alternatives = response.getBody() != null ? response.getBody().get("alternatives") : null;
        return alternatives != null ? alternatives.toString() : "";
    }

    public String explainAnswer(QuestionCorrectionDto correctionDto) throws IOException, InterruptedException {
        log.info("Delegating explain answer to myquiz-app");
        Long questionId = correctionDto.getOriginalQuestion() != null ? correctionDto.getOriginalQuestion().getId() : 0L;
        HttpEntity<QuestionCorrectionDto> entity = sessionService.createAuthorizedRequest(correctionDto);
        ResponseEntity<java.util.Map> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId + "/correction/explanation",
                HttpMethod.POST,
                entity,
                java.util.Map.class
        );
        Object explanation = response.getBody() != null ? response.getBody().get("explanation") : null;
        return explanation != null ? explanation.toString() : "";
    }
}
