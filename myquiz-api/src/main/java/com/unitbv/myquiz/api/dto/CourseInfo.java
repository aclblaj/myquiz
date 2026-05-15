package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Helper class to hold minimal course information (id + name) for dropdowns and filter lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfo {
    private Long id;
    private String name;

    public static CourseInfo from(CourseDto courseDto) {
        if (courseDto == null) {
            return null;
        }
        return CourseInfo.builder().id(courseDto.getId()).name(courseDto.getCourse()).build();
    }
}

