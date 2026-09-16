package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable metrics produced by the duplicate-recompute application operation.
 * The course-specific response adds scope and timing information around this summary.
 */
@Schema(description = "Metrics produced by a duplicate-recompute operation")
public record DuplicateRecomputeSummaryDto(
        int totalQuestions,
        int multichoiceQuestions,
        int truefalseQuestions,
        int duplicateLinksRemoved,
        int duplicateErrorsRemoved,
        int duplicateErrorsCreated
) {
}
