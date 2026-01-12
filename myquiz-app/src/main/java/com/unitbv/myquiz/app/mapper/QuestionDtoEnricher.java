package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to enrich QuestionDto with error information.
 * This is separated from QuestionMapper to avoid MapStruct compilation issues
 * with cross-module dependencies.
 */
@Component
public class QuestionDtoEnricher {

    /**
     * Populate the errors field in QuestionDto from Question entity.
     *
     * @param dto The QuestionDto to enrich
     * @param question The Question entity with potential errors
     */
    public void enrichWithErrors(QuestionDto dto, Question question) {
        if (question.getQuestionErrors() != null && !question.getQuestionErrors().isEmpty()) {
            List<QuestionErrorDto> errorDtos = new ArrayList<>();
            for (QuestionError qe : question.getQuestionErrors()) {
                if (qe.getQuizError() != null) {
                    QuestionErrorDto errorDto = new QuestionErrorDto(
                        qe.getQuizError().getId(),
                        qe.getQuizError().getDescription(),
                        qe.getQuizError().getRowNumber()
                    );
                    errorDtos.add(errorDto);
                }
            }
            dto.setErrors(errorDtos);
        }
    }

    /**
     * Enrich a list of QuestionDto objects with their errors.
     *
     * @param dtos List of QuestionDto objects
     * @param questions Corresponding list of Question entities
     */
    public void enrichListWithErrors(List<QuestionDto> dtos, List<Question> questions) {
        if (dtos.size() != questions.size()) {
            throw new IllegalArgumentException("DTO and entity lists must have the same size");
        }

        for (int i = 0; i < dtos.size(); i++) {
            enrichWithErrors(dtos.get(i), questions.get(i));
        }
    }
}

