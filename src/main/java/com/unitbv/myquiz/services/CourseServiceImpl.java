package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.CourseDto;
import com.unitbv.myquiz.entities.Course;
import com.unitbv.myquiz.repositories.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<CourseDto> getAllCourses() {
        List<CourseDto> courseDtos = new ArrayList<>();
        List<Course> courses = (List<Course>) courseRepository.findAll();
        courses.stream().forEach(
                course -> {
                    CourseDto courseDto = new CourseDto(
                            course.getId(),
                            course.getCourse(),
                            course.getDescription(),
                            course.getUniversityYear(),
                            course.getSemester(),
                            course.getStudy_year()
                    );
                    courseDtos.add(courseDto);
                }
        );
        return courseDtos;
    }

    @Override
    public void deleteCourseById(Long id) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            courseRepository.deleteById(id);
        } else {
            log.error("course {} not found", id);
        }
    }


}
