package com.unitbv.myquiz.app.upload.application.support;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.services.CourseService;
import org.springframework.stereotype.Component;

@Component
public class UploadCourseLookupSupport {
    private final CourseService courseService;

    public UploadCourseLookupSupport(CourseService courseService) {
        this.courseService = courseService;
    }

    public CourseDto findCourseById(Long courseId) {
        CourseDto directLookup = courseService.findById(courseId);
        if (directLookup != null) {
            return directLookup;
        }
        return courseService.getAllCourses().stream().filter(c -> c.getId().equals(courseId)).findFirst().orElse(null);
    }
}

