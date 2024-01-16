package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends PagingAndSortingRepository<Author, Long>, CrudRepository<Author, Long> {
    List<Author> findAll();

    Optional<Author> findByName(String name);
}
