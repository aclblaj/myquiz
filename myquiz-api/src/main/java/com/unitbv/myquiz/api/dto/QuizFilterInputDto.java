package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuizFilterInputDto {
    private int page;
    private int pageSize;
    private String course;
}
