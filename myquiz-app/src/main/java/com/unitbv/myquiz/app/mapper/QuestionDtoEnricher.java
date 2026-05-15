package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionDuplicateDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to enrich QuestionDto with error and duplicate information.
 */
@Component
public final class QuestionDtoEnricher {

    public void enrichWithErrors(QuestionDto dto, Question question) {
        if (dto == null || question == null) {
            return;
        }
        dto.setDuplicateCount(question.getDuplicateCount());

        if (question.getQuestionErrors() != null && !question.getQuestionErrors().isEmpty()) {
            List<QuestionErrorDto> errorDtos = new ArrayList<>();
            for (QuestionError qe : question.getQuestionErrors()) {
                if (qe.getDescription() != null) {
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

        // Enrich with duplicates from both directions
        List<QuestionDuplicateDto> duplicateDtos = new ArrayList<>();
        if (question.getDuplicateLinks() != null) {
            for (QuestionDuplicate dup : question.getDuplicateLinks()) {
                Question other = dup.getDuplicateQuestion();
                if (other != null) {
                    duplicateDtos.add(buildDuplicateDto(dup.getId(), other));
                }
            }
        }
        if (question.getDuplicateOfLinks() != null) {
            for (QuestionDuplicate dup : question.getDuplicateOfLinks()) {
                Question other = dup.getQuestion();
                if (other != null) {
                    duplicateDtos.add(buildDuplicateDto(dup.getId(), other));
                }
            }
        }
        dto.setDuplicates(duplicateDtos);
    }

    private QuestionDuplicateDto buildDuplicateDto(Long linkId, Question other) {
        QuestionDuplicateDto d = new QuestionDuplicateDto();
        d.setDuplicateLinkId(linkId);
        d.setQuestionId(other.getId());
        d.setTitle(other.getTitle());
        d.setText(other.getText());
        d.setRow(other.getCrtNo());
        d.setType(other.getType());
        d.setResponse1(other.getResponse1());
        d.setResponse2(other.getResponse2());
        d.setResponse3(other.getResponse3());
        d.setResponse4(other.getResponse4());
        QuestionBankAuthor qa = other.getQuestionBankAuthor();
        if (qa != null) {
            if (qa.getAuthor() != null) d.setAuthorName(qa.getAuthor().getName());
            if (qa.getQuestionBank() != null) {
                d.setQuestionBankName(qa.getQuestionBank().getName());
                d.setCourse(qa.getQuestionBank().getCourseName());
            }
        }
        return d;
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

