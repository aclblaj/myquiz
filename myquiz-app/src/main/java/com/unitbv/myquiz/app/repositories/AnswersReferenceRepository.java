package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.AnswersReference;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswersReferenceRepository extends CrudRepository<AnswersReference, Long> {
}

