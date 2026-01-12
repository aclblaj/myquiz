package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuizAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


/**
 * Repository interface for QuizAuthor entity operations.
 * Uses Specification pattern.
 */
@Repository
public interface QuizAuthorRepository extends JpaRepository<QuizAuthor, Long>, JpaSpecificationExecutor<QuizAuthor> {

}



