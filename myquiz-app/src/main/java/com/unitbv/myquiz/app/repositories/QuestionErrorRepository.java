package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuestionError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionErrorRepository extends
    JpaRepository<QuestionError, Long>,
    JpaSpecificationExecutor<QuestionError> {

    long countByQuestionQuestionBankAuthorId(Long questionBankAuthorId);

    long countByQuestionQuestionBankAuthorAuthorIdAndQuestionQuestionBankAuthorQuestionBankId(Long authorId, Long questionBankId);

    List<QuestionError> findByQuestionQuestionBankAuthorId(Long questionBankAuthorId);

    List<QuestionError> findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(Long questionBankId, Long authorId);

    List<QuestionError> findByQuestionQuestionBankAuthorQuestionBankId(Long questionBankId);

    List<QuestionError> findByQuestionIdInAndDescriptionStartingWith(List<Long> questionIds, String descriptionPrefix);

    long deleteByQuestionIdInAndDescriptionStartingWith(List<Long> questionIds, String descriptionPrefix);
}
