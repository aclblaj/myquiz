package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Question;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends PagingAndSortingRepository<Question, Long>, CrudRepository<Question, Long> {
    Optional<Question> findById(Long id);

    List<Question> findAllByAuthorId(Long authorId);

    @Query(value = "SHOW server_encoding", nativeQuery = true)
    String getEncoding();

}
