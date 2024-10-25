package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.QuizAuthor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAuthorRepository extends PagingAndSortingRepository<QuizAuthor, Long>, CrudRepository<QuizAuthor, Long> {
    List<QuizAuthor> findByAuthorId(Long authorId);
}
