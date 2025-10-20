package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuizRepository extends PagingAndSortingRepository<Quiz, Long>, CrudRepository<Quiz, Long> {
    Optional<Quiz> findByNameAndCourseAndAndYear(String name, String course, Long year);
    List<Quiz> findAll();

    @Query("SELECT DISTINCT q FROM Quiz q " +
           "LEFT JOIN FETCH q.quizAuthors qa " +
           "WHERE q.course = :selectedCourse")
    List<Quiz> findQuizIdByCourse(@Param("selectedCourse") String selectedCourse);

    Quiz findByQuizAuthors(Set<QuizAuthor> quizAuthors);

    List<Quiz> findAllByCourse(String course);
}
