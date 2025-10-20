package com.unitbv.myquiz.services;

import com.unitbv.myquizapi.dto.CourseDto;

import java.util.List;

public interface CourseService {
    List<CourseDto> getAllCourses();

    void deleteCourseById(Long id);

    void deleteCourse(String selectedCourse);

    CourseDto findById(Long id);

    void updateCourse(Long id, com.unitbv.myquizapi.dto.CourseDto courseDto);

    CourseDto createCourse(CourseDto courseDto);

    CourseDto createCourseIfNotExists(CourseDto courseDto);
}
