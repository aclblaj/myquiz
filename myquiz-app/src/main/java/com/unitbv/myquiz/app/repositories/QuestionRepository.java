package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Question;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Question entity operations.
 * Uses Specification pattern for flexible and composable queries.
 * All custom queries have been migrated to QuestionSpecification.
 *
 * @see com.unitbv.myquiz.app.specifications.QuestionSpecification
 * @since December 28, 2025 - Migrated to Specification pattern
 */
@Repository
public interface QuestionRepository extends PagingAndSortingRepository<Question, Long>, CrudRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    /**
     * Get database encoding setting.
     *
     * @return Database server encoding
     */
    @Query(value = "SHOW server_encoding", nativeQuery = true)
    String getEncoding();

    void deleteAll();

    /*
     * REMOVED METHODS - Now use QuestionSpecification instead:
     *
     * Replace: findById(Long id)
     * With: findOne(QuestionSpecification.byId(id))
     *
     * Replace: deleteQuestionsByQuizAuthorId(Long quizAuthorId)
     * With: deleteAll(findAll(QuestionSpecification.byQuizAuthorId(quizAuthorId)))
     */
}
