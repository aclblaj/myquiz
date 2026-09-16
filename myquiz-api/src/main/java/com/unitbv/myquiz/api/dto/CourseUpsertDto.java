package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request-only contract for creating or updating a course. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Course data accepted when creating or updating a course")
public class CourseUpsertDto {

    @NotBlank(message = "Course cannot be blank")
    @Size(max = 200, message = "Course cannot exceed 200 characters")
    @Schema(description = "Course name", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String course;

    @Size(max = 500, message = "Course description cannot exceed 500 characters")
    @Schema(description = "Course description", maxLength = 500)
    private String description;

    @Size(max = 20, message = "University year cannot exceed 20 characters")
    @Schema(description = "University year", maxLength = 20)
    private String universityYear;

    @Size(max = 20, message = "Semester cannot exceed 20 characters")
    @Schema(description = "Semester", maxLength = 20)
    private String semester;

    public CourseDto toCourseDto() {
        return new CourseDto(null, course, description, universityYear, semester);
    }

    public static CourseUpsertDto from(CourseDto source) {
        if (source == null) {
            return null;
        }
        return new CourseUpsertDto(
                source.getCourse(),
                source.getDescription(),
                source.getUniversityYear(),
                source.getSemester()
        );
    }
}
