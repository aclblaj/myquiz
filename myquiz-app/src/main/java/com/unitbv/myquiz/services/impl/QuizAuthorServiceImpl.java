package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.services.QuizAuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizAuthorServiceImpl implements QuizAuthorService {
    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    public QuizAuthorServiceImpl(QuizAuthorRepository quizAuthorRepository) {
        this.quizAuthorRepository = quizAuthorRepository;
    }

    public void deleteAll() {
        quizAuthorRepository.deleteAll();
    }

    public List<QuizAuthor> getQuizAuthorsForAuthorName(String authorName) {
        return quizAuthorRepository.findByAuthor_NameContainsIgnoreCase(authorName);
    }

    public List<QuizAuthor> getQuizAuthorsWithQuestionsAndErrorsByQuizId(Long quizId) {
        return quizAuthorRepository.findWithQuestionsAndQuizErrorsByQuizId(quizId);
    }
}
