package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

@Schema(description = "Result summary returned after recomputing duplicate links for a course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"courseId", "courseName", "totalQuestions", "durationMs"})
public class CourseDuplicateRecomputeResultDto {

    @Schema(description = "ID of the processed course")
    private Long courseId;

    @Schema(description = "Name of the processed course")
    private String courseName;

    @Schema(description = "Total number of questions processed")
    private int totalQuestions;

    @Schema(description = "Number of processed multiple choice questions")
    private int multichoiceQuestions;

    @Schema(description = "Number of processed true/false questions")
    private int truefalseQuestions;

    @Schema(description = "Number of stale duplicate links removed during recomputation")
    private int duplicateLinksRemoved;

    @Schema(description = "Number of duplicate-related validation errors removed")
    private int duplicateErrorsRemoved;

    @Schema(description = "Number of duplicate-related validation errors created")
    private int duplicateErrorsCreated;

    @Schema(description = "Timestamp when recomputation started")
    private OffsetDateTime startedAt;

    @Schema(description = "Timestamp when recomputation ended")
    private OffsetDateTime endedAt;

    @Schema(description = "Total recomputation duration in milliseconds")
    private long durationMs;

    public static CourseDuplicateRecomputeResultDto from(
            Long courseId,
            String courseName,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            long durationMs,
            DuplicateRecomputeSummaryDto summary
    ) {
        if (summary == null) {
            throw new IllegalArgumentException("Duplicate recompute summary cannot be null");
        }
        CourseDuplicateRecomputeResultDto result = new CourseDuplicateRecomputeResultDto();
        result.courseId = courseId;
        result.courseName = courseName;
        result.startedAt = startedAt;
        result.endedAt = endedAt;
        result.durationMs = durationMs;
        result.totalQuestions = summary.totalQuestions();
        result.multichoiceQuestions = summary.multichoiceQuestions();
        result.truefalseQuestions = summary.truefalseQuestions();
        result.duplicateLinksRemoved = summary.duplicateLinksRemoved();
        result.duplicateErrorsRemoved = summary.duplicateErrorsRemoved();
        result.duplicateErrorsCreated = summary.duplicateErrorsCreated();
        return result;
    }
}

