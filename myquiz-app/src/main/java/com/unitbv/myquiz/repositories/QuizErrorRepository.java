package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.QuizError;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface QuizErrorRepository extends PagingAndSortingRepository<QuizError, Long>, CrudRepository<QuizError, Long> {
    void deleteQuizErrorsByQuizAuthorId(Long quizAuthorId);

    List<QuizError> findByQuizAuthorId(Long id);
}
