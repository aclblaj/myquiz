package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.Course;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Course entity operations.
 * Provides methods for course management and retrieval.
 */
@Repository
public interface CourseRepository extends PagingAndSortingRepository<Course, Long>, JpaSpecificationExecutor<Course>, CrudRepository<Course, Long> {
    List<Course> findAll();
}
