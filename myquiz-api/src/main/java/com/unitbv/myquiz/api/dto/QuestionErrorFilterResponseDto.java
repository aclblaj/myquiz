package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canonical response DTO for question error filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class QuestionErrorFilterResponseDto extends BaseFilterDto {
    private String course;
    private Long selectedCourseId;
    private String authorName;
    private Long selectedQuestionBankId;
    private List<QuestionErrorDto> questionErrors;
    private ArrayList<String> authorNames;
    private Map<String, List<QuestionErrorDto>> questionErrorsByAuthor;

    @JsonProperty("courses")
    public List<CourseInfo> getCourses() {
        return getFilterCourses();
    }

    @JsonProperty("courses")
    public void setCourses(List<CourseInfo> courses) {
        setFilterCourses(courses);
    }

    @JsonProperty("questionBanks")
    public List<QuestionBankInfo> getQuestionBanks() {
        return getFilterQuestionBanks();
    }

    @JsonProperty("questionBanks")
    public void setQuestionBanks(List<QuestionBankInfo> questionBanks) {
        setFilterQuestionBanks(questionBanks);
    }
}
