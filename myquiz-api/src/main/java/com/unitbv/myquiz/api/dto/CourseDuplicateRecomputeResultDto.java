package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("courseId")
    @Schema(description = "ID of the processed course")
    private Long courseId;

    @JsonProperty("courseName")
    @Schema(description = "Name of the processed course")
    private String courseName;

    @JsonProperty("totalQuestions")
    @Schema(description = "Total number of questions processed")
    private int totalQuestions;

    @JsonProperty("multichoiceQuestions")
    @Schema(description = "Number of processed multiple choice questions")
    private int multichoiceQuestions;

    @JsonProperty("truefalseQuestions")
    @Schema(description = "Number of processed true/false questions")
    private int truefalseQuestions;

    @JsonProperty("duplicateLinksRemoved")
    @Schema(description = "Number of stale duplicate links removed during recomputation")
    private int duplicateLinksRemoved;

    @JsonProperty("duplicateErrorsRemoved")
    @Schema(description = "Number of duplicate-related validation errors removed")
    private int duplicateErrorsRemoved;

    @JsonProperty("duplicateErrorsCreated")
    @Schema(description = "Number of duplicate-related validation errors created")
    private int duplicateErrorsCreated;

    @JsonProperty("startedAt")
    @Schema(description = "Timestamp when recomputation started")
    private OffsetDateTime startedAt;

    @JsonProperty("endedAt")
    @Schema(description = "Timestamp when recomputation ended")
    private OffsetDateTime endedAt;

    @JsonProperty("durationMs")
    @Schema(description = "Total recomputation duration in milliseconds")
    private long durationMs;
}

