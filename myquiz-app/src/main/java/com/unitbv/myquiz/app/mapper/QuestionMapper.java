package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.dto.QuestionDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface QuestionMapper {
    @Mapping(target = "authorName", source = "quizAuthor.author.name")
    @Mapping(target = "quizName", source = "quizAuthor.quiz.name")
    @Mapping(target = "course", source = "quizAuthor.quiz.course")
    @Mapping(target = "row", source = "crtNo")
    QuestionDto toDto(Question question);

    @Mapping(target = "crtNo", source = "row")
    @Mapping(target = "questionErrors", ignore = true)
    Question toEntity(QuestionDto dto);
}
