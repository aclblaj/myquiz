package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuizError;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for QuizError entity operations.
 * Uses Specification pattern for flexible and composable queries.
 * All custom queries have been migrated to QuizErrorSpecification.
 *
 * @see com.unitbv.myquiz.app.specifications.QuizErrorSpecification
 * @since December 28, 2025 - Migrated to Specification pattern
 */
@Repository
public interface QuizErrorRepository extends
    PagingAndSortingRepository<QuizError, Long>,
    JpaSpecificationExecutor<QuizError>,
    CrudRepository<QuizError, Long> {

    void deleteAll();

    /*
     * REMOVED METHODS - Now use QuizErrorSpecification instead:
     *
     * Replace: deleteQuizErrorsByQuizAuthorId(Long quizAuthorId)
     * With: deleteAll(findAll(QuizErrorSpecification.byQuizAuthor(quizAuthorId)))
     */
}
