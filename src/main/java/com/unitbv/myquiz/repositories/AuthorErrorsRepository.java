package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.AuthorErrors;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorErrorsRepository extends PagingAndSortingRepository<AuthorErrors, Long> {
    void save(AuthorErrors authorErrors);
}
