package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Quiz;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface QuizRepository extends PagingAndSortingRepository<Quiz, Long>, CrudRepository<Quiz, Long> {
    Optional<Quiz> findByNameAndCourseAndAndYear(String name, String course, Long year);
}
