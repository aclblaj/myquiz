package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Quiz;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Quiz entity operations.
 * Uses Specification pattern for flexible and composable queries.
 * All custom queries have been migrated to QuizSpecification.
 *
 * @see com.unitbv.myquiz.app.specifications.QuizSpecification
 * @since December 28, 2025 - Migrated to Specification pattern
 */
@Repository
public interface QuizRepository extends
    PagingAndSortingRepository<Quiz, Long>,
    JpaSpecificationExecutor<Quiz>,
    CrudRepository<Quiz, Long> {

    List<Quiz> findAll();

    /*
     * REMOVED METHODS - Now use QuizSpecification instead:
     *
     * Replace: findByNameAndCourseAndYear(String name, String course, Long year)
     * With: findOne(QuizSpecification.byNameAndCourseAndYear(name, course, year))
     *
     * Replace: findQuizIdByCourse(String course)
     * With: findAll(QuizSpecification.byCourse(course))
     *
     * Replace: findAllByCourse(String course)
     * With: findAll(QuizSpecification.byCourse(course))
     *
     * Replace: findByQuizAuthors_Id(Long quizAuthorId)
     * With: findOne(QuizSpecification.byQuizAuthorId(quizAuthorId))
     *
     * Replace: findByQuizAuthors(Set<QuizAuthor> quizAuthors)
     * With: Use QuizSpecification with appropriate filters
     */
}
