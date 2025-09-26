package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.CourseDto;
import com.unitbv.myquiz.dto.QuizDto;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.util.TemplateType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class QuizServiceImpl implements QuizService{

    QuizRepository quizRepository;
    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository, QuizAuthorRepository quizAuthorRepository) {
        this.quizRepository = quizRepository;
        this.quizAuthorRepository = quizAuthorRepository;
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

    @Override
    public List<QuizDto> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAll();
        quizzes.sort((q1, q2) -> {
            return q1.getCourse().compareTo(q2.getCourse());
        });
        return QuizDto.toDtoList(quizzes);
    }

    @Override
    @Transactional
    public void deleteQuizById(Long id) {
        quizAuthorRepository.deleteAllByQuizId(id);
        quizRepository.deleteById(id);
    }

    @Override
    public QuizDto getQuizById(Long id) {
        Optional<Quiz> quiz = quizRepository.findById(id);
        if (quiz.isPresent()) {
            return new QuizDto(quiz.get());
        }
        return null;
    }

    @Override
    public int getCompareTo(QuizDto q1, QuizDto q2) {
        if (q1 == null || q2 == null) {
            return 0;
        }
        return q1.getCourse().compareTo(q2.getCourse());
    }

}
