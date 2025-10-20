package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquizapi.dto.QuizDto;

import java.util.List;

public interface QuizService {
    Quiz createQuizz(String course, String quizz, long year);

    void deleteAll();

    List<QuizDto> getAllQuizzes();

    void deleteQuizById(Long id);

    QuizDto getQuizById(Long id);

    int getCompareTo(QuizDto q1, QuizDto q2);
    
    Quiz updateQuiz(Long id, String course, String name, Long year);
    
    List<QuizDto> getQuizzesByCourse(String course);

    List<QuizDto> getQuizzesByCourseId(Long courseId);

    List<?> getQuestionsByQuizId(Long id);
}
