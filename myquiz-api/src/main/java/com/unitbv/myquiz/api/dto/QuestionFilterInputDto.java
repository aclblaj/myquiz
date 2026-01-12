package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFilterInputDto {
    private String course;
    private String author;
    private Integer page;
    private Integer pageSize;
    private Long quiz;
    private QuestionType questionType;
}
