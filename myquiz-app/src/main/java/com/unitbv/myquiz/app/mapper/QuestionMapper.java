package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(target = "author", source = "questionBankAuthor")
    @Mapping(target = "questionBankName", source = "questionBankAuthor.questionBank.name")
    @Mapping(target = "questionBankId", source = "questionBankAuthor.questionBank.id")
    @Mapping(target = "course", source = "questionBankAuthor.questionBank.course.course")
    @Mapping(target = "row", source = "crtNo")
    @Mapping(target = "duplicateCount", expression = "java(question.getDuplicateCount())")
    @Mapping(target = "answerReferenceText", source = "answerReferenceText")
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "duplicates", ignore = true)
    QuestionDto toDto(Question question);

    @Mapping(target = "crtNo", source = "row")
    @Mapping(target = "questionErrors", ignore = true)
    @Mapping(target = "answersReference", ignore = true)
    @Mapping(target = "questionBankAuthor", ignore = true)
    @Mapping(target = "duplicateLinks", ignore = true)
    @Mapping(target = "duplicateOfLinks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Question toEntity(QuestionDto dto);

    /**
     * Custom mapping helper for extracting AuthorInfo from QuestionBankAuthor.
     */
    default AuthorInfo mapAuthor(QuestionBankAuthor qba) {
        if (qba == null || qba.getAuthor() == null) {
            return null;
        }
        Author author = qba.getAuthor();
        return AuthorInfo.builder()
                .id(author.getId())
                .name(author.getName())
                .initials(author.getInitials())
                .build();
    }

    /**
     * Helper method to map answerReferenceText after the main mapping (retained for backward compatibility).
     */
    default QuestionDto toDtoWithAnswerReference(Question question) {
        return toDto(question);
    }
}
