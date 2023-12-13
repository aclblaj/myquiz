package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Question;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends PagingAndSortingRepository<Question, Long> {
    Question save(Question question);
    Optional<Question> findById(Long id);

    @Query(value = "SHOW server_encoding", nativeQuery = true)
    String getEncoding();

    void delete(Question question);
}
