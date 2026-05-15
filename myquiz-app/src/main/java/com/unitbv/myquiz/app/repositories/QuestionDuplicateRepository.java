package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionDuplicateRepository extends JpaRepository<QuestionDuplicate, Long> {

    List<QuestionDuplicate> findByQuestionIdOrDuplicateQuestionId(Long questionId, Long duplicateQuestionId);

    List<QuestionDuplicate> findByQuestionIdInOrDuplicateQuestionIdIn(Collection<Long> questionIds, Collection<Long> duplicateQuestionIds);

    boolean existsByQuestionIdAndDuplicateQuestionId(Long questionId, Long duplicateQuestionId);

    long countByQuestionIdOrDuplicateQuestionId(Long questionId, Long duplicateQuestionId);

    long deleteByQuestionIdAndDuplicateQuestionId(Long questionId, Long duplicateQuestionId);

    long deleteByQuestionIdOrDuplicateQuestionId(Long questionId, Long duplicateQuestionId);

    long deleteByQuestionIdInOrDuplicateQuestionIdIn(Collection<Long> questionIds, Collection<Long> duplicateQuestionIds);
}
