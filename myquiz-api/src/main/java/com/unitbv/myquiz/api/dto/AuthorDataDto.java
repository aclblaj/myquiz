package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorDataDto {
    private List<QuizDto> quizDtos;
    private QuizDto quizDto;
    private List<AuthorInfo> authorsList;
    private AuthorDto authorDTO;
}
