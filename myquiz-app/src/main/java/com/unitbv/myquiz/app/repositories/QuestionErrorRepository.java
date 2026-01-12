package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuestionError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for QuestionError junction entity.
 * Manages the many-to-many relationship between Questions and QuizErrors.
 */
@Repository
public interface QuestionErrorRepository extends
    JpaRepository<QuestionError, Long>,
    JpaSpecificationExecutor<QuestionError> {
}

