package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends PagingAndSortingRepository<Author, Long>, CrudRepository<Author, Long> {
}
