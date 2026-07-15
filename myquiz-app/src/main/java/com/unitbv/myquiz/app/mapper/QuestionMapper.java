package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.dto.QuestionDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring", imports = {AuthorInfo.class, Author.class})
public interface QuestionMapper {
    @Mapping(target = "author", expression = "java(question.getQuestionBankAuthor() != null && question.getQuestionBankAuthor().getAuthor() != null ? AuthorInfo.builder().id(question.getQuestionBankAuthor().getAuthor().getId()).name(question.getQuestionBankAuthor().getAuthor().getName()).initials(question.getQuestionBankAuthor().getAuthor().getInitials()).build() : null)")
    @Mapping(target = "questionBankName", source = "questionBankAuthor.questionBank.name")
    @Mapping(target = "questionBankId", source = "questionBankAuthor.questionBank.id")
    @Mapping(target = "course", source = "questionBankAuthor.questionBank.course.course")
    @Mapping(target = "row", source = "crtNo")
    @Mapping(target = "duplicateCount", ignore = true)
    QuestionDto toDto(Question question);

    @Mapping(target = "crtNo", source = "row")
    @Mapping(target = "questionErrors", ignore = true)
    @Mapping(target = "answersReference", ignore = true)
    @Mapping(target = "questionBankAuthor", ignore = true)
    Question toEntity(QuestionDto dto);

    // Helper method to map answerReferenceText after the main mapping
    default QuestionDto toDtoWithAnswerReference(Question question) {
        QuestionDto dto = toDto(question);
        if (question != null && question.getAnswerReferenceText() != null) {
            dto.setAnswerReferenceText(question.getAnswerReferenceText());
        }
        return dto;
    }
}
