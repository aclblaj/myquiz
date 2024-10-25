package com.unitbv.myquiz.services;

import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuizAuthorService {

    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    public QuizAuthorService(QuizAuthorRepository quizAuthorRepository) {
        this.quizAuthorRepository = quizAuthorRepository;
    }

    public void deleteAll() {
        quizAuthorRepository.deleteAll();
    }
}
