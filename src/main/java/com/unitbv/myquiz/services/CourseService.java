package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.CourseDto;

import java.util.List;

public interface CourseService {
    List<CourseDto> getAllCourses();

    void deleteCourseById(Long id);
}
