package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.util.TemplateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class QuizServiceImpl implements QuizService{

    QuizRepository quizRepository;

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Override
    public Quiz createQuizz(String courseName, String quizName, long year) {
        Quiz quiz;
        Optional<Quiz> searchQuiz = quizRepository.findByNameAndCourseAndAndYear(quizName, courseName, year);
        if (!searchQuiz.isPresent()) {
            Quiz newQuiz = new Quiz();
            newQuiz.setName(quizName);
            newQuiz.setCourse(courseName);
            newQuiz.setYear(year);
            quiz = quizRepository.save(newQuiz);
        } else {
            quiz = searchQuiz.get();
        }
        return quiz;
    }

    @Override
    public void deleteAll() {
        quizRepository.deleteAll();
    }
}
