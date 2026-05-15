package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuestionBank;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for QuestionBank entity operations.
 * Uses Specification pattern for flexible and composable queries.
 * All custom queries use QuestionBankSpecification.
 */
@Repository
public interface QuestionBankRepository extends
    PagingAndSortingRepository<QuestionBank, Long>,
    JpaSpecificationExecutor<QuestionBank>,
    CrudRepository<QuestionBank, Long> {

    List<QuestionBank> findAll();

}

