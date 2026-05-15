package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Shared base DTO for filter option lists used by filter response payloads.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BaseFilterDto extends BasePaginationDto {
    @JsonIgnore
    private List<AuthorInfo> filterAuthors;

    @JsonIgnore
    private List<CourseInfo> filterCourses;

    @JsonIgnore
    private List<QuestionBankInfo> filterQuestionBanks;
}

