package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.AuthorError;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorErrorRepository extends PagingAndSortingRepository<AuthorError, Long>, CrudRepository<AuthorError, Long> {
}
