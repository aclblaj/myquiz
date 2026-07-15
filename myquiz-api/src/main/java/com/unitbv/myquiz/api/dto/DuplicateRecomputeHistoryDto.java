package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * DTO representing a saved duplicate-recompute run entry stored in history.
 * Contains all execution parameters and result metrics.
 */
@Schema(description = "Saved duplicate recompute history entry with parameters, algorithm and result metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "courseId", "strategy", "savedAt"})
public class DuplicateRecomputeHistoryDto {

    @JsonProperty("id")
    @Schema(description = "Unique identifier of this history entry")
    private Long id;

    @JsonProperty("courseId")
    @Schema(description = "ID of the course that was recomputed")
    private Long courseId;

    @JsonProperty("courseName")
    @Schema(description = "Name of the course that was recomputed")
    private String courseName;

    @JsonProperty("questionBankId")
    @Schema(description = "ID of the question bank scope (null = entire course)")
    private Long questionBankId;

    @JsonProperty("authorId")
    @Schema(description = "ID of the author scope (null = all authors)")
    private Long authorId;

    @JsonProperty("strategy")
    @Schema(description = "Algorithm used for duplicate detection (e.g. string-equality, levenshtein, jaro-winkler)")
    private String strategy;

    @JsonProperty("totalQuestions")
    @Schema(description = "Total number of questions processed")
    private int totalQuestions;

    @JsonProperty("multichoiceQuestions")
    @Schema(description = "Number of multiple choice questions processed")
    private int multichoiceQuestions;

    @JsonProperty("truefalseQuestions")
    @Schema(description = "Number of true/false questions processed")
    private int truefalseQuestions;

    @JsonProperty("duplicateLinksRemoved")
    @Schema(description = "Number of stale duplicate links removed")
    private int duplicateLinksRemoved;

    @JsonProperty("duplicateErrorsRemoved")
    @Schema(description = "Number of duplicate-related errors removed")
    private int duplicateErrorsRemoved;

    @JsonProperty("duplicateErrorsCreated")
    @Schema(description = "Number of duplicate-related errors created")
    private int duplicateErrorsCreated;

    @JsonProperty("startedAt")
    @Schema(description = "Timestamp when the recomputation run started")
    private OffsetDateTime startedAt;

    @JsonProperty("endedAt")
    @Schema(description = "Timestamp when the recomputation run ended")
    private OffsetDateTime endedAt;

    @JsonProperty("durationMs")
    @Schema(description = "Duration of the recomputation run in milliseconds")
    private long durationMs;

    @JsonProperty("savedAt")
    @Schema(description = "Timestamp when this history entry was saved")
    private OffsetDateTime savedAt;
}
