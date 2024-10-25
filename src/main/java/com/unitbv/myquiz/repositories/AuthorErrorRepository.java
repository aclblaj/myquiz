package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.QuizError;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorErrorRepository extends PagingAndSortingRepository<QuizError, Long>, CrudRepository<QuizError, Long> {
    void deleteAll();
    List<QuizError> findAllByQuizAuthor_Author_NameContainsIgnoreCase(String authorName);
    List<QuizError> findAllByOrderByQuizAuthor_Author_NameAsc();
}
