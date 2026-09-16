package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for duplicate statistics of a course.
 */
@Schema(description = "Aggregate duplicate statistics for a course, question bank, or author scope")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DuplicateStatisticsDto {

    @Schema(description = "Course name for the reported scope")
    private String courseName;

    @Schema(description = "Total number of questions in the selected scope")
    private int totalQuestions;

    @Schema(description = "Number of questions currently marked with duplicate-related validation errors")
    private int questionsWithDuplicateErrors;

    @Schema(description = "Number of duplicate links in the selected scope")
    private long duplicateLinks;
}
