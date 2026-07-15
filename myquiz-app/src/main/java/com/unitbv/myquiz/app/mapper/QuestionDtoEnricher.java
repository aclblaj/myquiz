package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionDuplicateDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.services.MyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to enrich QuestionDto with error and duplicate information.
 */
@Component
public final class QuestionDtoEnricher {

    Logger logger = LoggerFactory.getLogger(QuestionDtoEnricher.class);

    private final QuestionDuplicateMapper questionDuplicateMapper;

    @Autowired
    public QuestionDtoEnricher(QuestionDuplicateMapper questionDuplicateMapper) {
        this.questionDuplicateMapper = questionDuplicateMapper;
    }

    public void enrichWithErrors(QuestionDto dto, Question question) {
        if (dto == null || question == null) {
            logger.error("QuestionDtoEnricher.enrichWithErrors error, null dto or question");
            return;
        }
        dto.setDuplicateCount(question.getDuplicateCount());

        List<QuestionErrorDto> errorDtos = new ArrayList<>();
        if (question.getQuestionErrors() != null && !question.getQuestionErrors().isEmpty()) {
            for (QuestionError qe : question.getQuestionErrors()) {
                if (qe.getDescription() != null && !MyUtil.isDuplicateValidationError(qe.getDescription())) {
                    QuestionErrorDto errorDto = new QuestionErrorDto(
                        qe.getId(),
                        qe.getDescription(),
                        qe.getRowNumber()
                    );
                    errorDtos.add(errorDto);
                }
            }
            dto.setErrors(errorDtos);
        }
        logger.debug("Enriched question {} with {} errors", question.getId(), errorDtos.size());
        enrichWithDuplicates(dto, question);
    }

    public void enrichWithDuplicates(QuestionDto dto, Question question) {
        if (dto == null || question == null) {
            return;
        }
        logger.debug("Enriching question {} with duplicates, {} duplicated links",
                     question.getId(),
                     question.getDuplicateLinks() != null ? question.getDuplicateLinks().size() : 0
        );
        // Enrich with duplicates from both directions
        List<QuestionDuplicateDto> duplicateDtos = new ArrayList<>();
        if (question.getDuplicateLinks() != null) {
            for (QuestionDuplicate dup : question.getDuplicateLinks()) {
                Question other = dup.getDuplicateQuestion();
                if (other != null) {
                    duplicateDtos.add(questionDuplicateMapper.toDuplicateDto(dup, other));
                }
            }
        }
        if (question.getDuplicateOfLinks() != null) {
            for (QuestionDuplicate dup : question.getDuplicateOfLinks()) {
                Question other = dup.getQuestion();
                if (other != null) {
                    duplicateDtos.add(questionDuplicateMapper.toDuplicateDto(dup, other));
                }
            }
        }
        logger.debug("Enriched question {} with {} duplicates", question.getId(), duplicateDtos.size());
        dto.setDuplicates(duplicateDtos);
    }

    public void enrichListWithErrors(List<QuestionDto> dtos, List<Question> questions) {
        if (dtos == null || questions == null) {
            throw new IllegalArgumentException("DTO and entity lists must not be null");
        }
        if (dtos.size() != questions.size()) {
            throw new IllegalArgumentException("DTO and entity lists must have the same size");
        }

        for (int i = 0; i < dtos.size(); i++) {
            enrichWithErrors(dtos.get(i), questions.get(i));
        }
    }
}

