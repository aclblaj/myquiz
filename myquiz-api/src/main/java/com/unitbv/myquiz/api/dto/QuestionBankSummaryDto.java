package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unitbv.myquiz.api.types.StudyYear;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Compact question bank representation for lists, filters and statistics. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compact question bank summary")
public class QuestionBankSummaryDto {

    private Long id;
    private String name;
    private String course;
    private Long courseId;
    @JsonProperty("study_year")
    private StudyYear studyYear;
    private Integer noAuthors;
    private int mcQuestionsCount;
    private int tfQuestionsCount;
    private Long numberOfDuplicates;

    public static QuestionBankSummaryDto from(QuestionBankDto source) {
        if (source == null) {
            return null;
        }
        return QuestionBankSummaryDto.builder()
                .id(source.getId())
                .name(source.getName())
                .course(source.getCourse())
                .courseId(source.getCourseId())
                .studyYear(source.getStudyYear())
                .noAuthors(source.getNoAuthors())
                .mcQuestionsCount(source.getMcQuestionsCount())
                .tfQuestionsCount(source.getTfQuestionsCount())
                .numberOfDuplicates(source.getNumberOfDuplicates())
                .build();
    }
}
