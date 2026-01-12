package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Author entity operations.
 * Provides methods for author management, searching, and pagination.
 */
@Repository
public interface AuthorRepository extends PagingAndSortingRepository<Author, Long>, JpaSpecificationExecutor<Author>, CrudRepository<Author, Long> {
    List<Author> findAll();

    Page<Author> findAll(Pageable pageable);

    void deleteAll();
}
