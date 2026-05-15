package com.unitbv.myquiz.app.mapper;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.entities.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "sources", ignore = true)
    CourseDto toDto(Course course);

    Course toEntity(CourseDto dto);
}
