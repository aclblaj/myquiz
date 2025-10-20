package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Course;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends CrudRepository<Course, Long> {
    Optional<Course> findByCourse(String selectedCourse);
}
