package com.unitbv.myquiz.api.dto;

import java.util.List;
import java.util.Map;

public class AuthorDetailsDto {
    private AuthorDto author;
    private List<QuizDto> quizzes;
    private Map<Long, List<QuestionDto>> questionsByQuiz;
    private Map<Long, List<AuthorErrorDto>> errorsByQuiz;

    public AuthorDto getAuthor() { return author; }
    public void setAuthor(AuthorDto author) { this.author = author; }

    public List<QuizDto> getQuizzes() { return quizzes; }
    public void setQuizzes(List<QuizDto> quizzes) { this.quizzes = quizzes; }

    public Map<Long, List<QuestionDto>> getQuestionsByQuiz() { return questionsByQuiz; }
    public void setQuestionsByQuiz(Map<Long, List<QuestionDto>> questionsByQuiz) { this.questionsByQuiz = questionsByQuiz; }

    public Map<Long, List<AuthorErrorDto>> getErrorsByQuiz() { return errorsByQuiz; }
    public void setErrorsByQuiz(Map<Long, List<AuthorErrorDto>> errorsByQuiz) { this.errorsByQuiz = errorsByQuiz; }
}

