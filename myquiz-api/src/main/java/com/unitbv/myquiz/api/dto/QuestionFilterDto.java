package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFilterDto {
    private List<QuestionDto> questions;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalItems;
    private String selectedCourse;
    private Long selectedAuthorId;
    private Long selectedQuizId;
    private String course;
    private List<AuthorInfo> authors;
    private List<QuizInfo> quizzes;
    private QuizDto selectedQuiz;
    private String authorName;
    private String[] courses;

}
