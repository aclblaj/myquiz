package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuestionDuplicateDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import org.springframework.stereotype.Component;

/**
 * Shared mapper for building {@link QuestionDuplicateDto} instances from {@link QuestionDuplicate}
 * links. Extracted into its own component (rather than living on QuestionBankService or
 * QuestionDtoEnricher) so both can depend on it without creating a circular bean dependency.
 */
@Component
public class QuestionDuplicateMapper {

    public QuestionDuplicateDto toDuplicateDto(QuestionDuplicate duplicateLink, Question question) {
        QuestionDuplicateDto duplicateDto = new QuestionDuplicateDto();
        duplicateDto.setId(duplicateLink != null ? duplicateLink.getId() : null);
        duplicateDto.setDuplicateLinkId(duplicateLink != null ? duplicateLink.getId() : null);
        duplicateDto.setStatus(duplicateLink != null && duplicateLink.getStatus() != null
                ? duplicateLink.getStatus() : ControllerSettings.DUPLICATE_STATUS_OPEN);
        duplicateDto.setQuestionId(question.getId());
        duplicateDto.setType(question.getType());
        duplicateDto.setTitle(question.getTitle());
        duplicateDto.setText(question.getText());
        duplicateDto.setResponse1(question.getResponse1());
        duplicateDto.setResponse2(question.getResponse2());
        duplicateDto.setResponse3(question.getResponse3());
        duplicateDto.setResponse4(question.getResponse4());
        duplicateDto.setRow(question.getCrtNo());

        // Populate both questions in the duplicate pair for display
        if (duplicateLink != null) {
            Question q1 = duplicateLink.getQuestion();
            Question q2 = duplicateLink.getDuplicateQuestion();
            if (q1 != null) {
                duplicateDto.setQuestion1Id(q1.getId());
                duplicateDto.setQuestion1Title(q1.getTitle());
                duplicateDto.setType1(q1.getType());
                duplicateDto.setResponse1Q1(q1.getResponse1());
                duplicateDto.setResponse2Q1(q1.getResponse2());
                duplicateDto.setResponse3Q1(q1.getResponse3());
                duplicateDto.setResponse4Q1(q1.getResponse4());
            }
            if (q2 != null) {
                duplicateDto.setQuestion2Id(q2.getId());
                duplicateDto.setQuestion2Title(q2.getTitle());
                duplicateDto.setType2(q2.getType());
                duplicateDto.setResponse1Q2(q2.getResponse1());
                duplicateDto.setResponse2Q2(q2.getResponse2());
                duplicateDto.setResponse3Q2(q2.getResponse3());
                duplicateDto.setResponse4Q2(q2.getResponse4());
            }
        }

        if (question.getQuestionBankAuthor() != null) {
            if (question.getQuestionBankAuthor().getQuestionBank() != null) {
                QuestionBank questionBank = question.getQuestionBankAuthor().getQuestionBank();
                duplicateDto.setQuestionBankName(questionBank.getName());
                if (questionBank.getCourse() != null) {
                    duplicateDto.setCourse(questionBank.getCourse().getCourse());
                }
            }
            if (question.getQuestionBankAuthor().getAuthor() != null) {
                duplicateDto.setAuthor(AuthorInfo.builder()
                        .id(question.getQuestionBankAuthor().getAuthor().getId())
                        .name(question.getQuestionBankAuthor().getAuthor().getName())
                        .initials(question.getQuestionBankAuthor().getAuthor().getInitials())
                        .build());
            }
        }

        return duplicateDto;
    }
}
