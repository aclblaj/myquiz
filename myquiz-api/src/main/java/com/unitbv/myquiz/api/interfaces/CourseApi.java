package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.CourseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface CourseApi {
    ResponseEntity<List<CourseDto>> getAllCourses();
    ResponseEntity<Void> deleteCourseById(@PathVariable Long id);
    ResponseEntity<CourseDto> findById(Long id);
    ResponseEntity<Void> updateCourse(@PathVariable Long id, CourseDto courseDto);
    ResponseEntity<CourseDto> createCourse(CourseDto courseDto);
}
