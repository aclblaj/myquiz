package com.unitbv.myquiz.dto;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
@lombok.ToString
public class QuizDto {
    private Long id;
    private String name;
    private String course;
    private Long year;
    private String sourceFile;
}
