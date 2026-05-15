package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Canonical response DTO for question filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestionFilterResponseDto extends BaseFilterDto {
    private List<QuestionDto> questions;
    private String selectedCourse;
    private Long selectedCourseId;
    private Long selectedAuthorId;
    private Long selectedQuestionBankId;
    private QuestionBankDto selectedQuestionBank;
    private String authorName;

    @JsonProperty("authors")
    public List<AuthorInfo> getAuthors() {
        return getFilterAuthors();
    }

    @JsonProperty("authors")
    public void setAuthors(List<AuthorInfo> authors) {
        setFilterAuthors(authors);
    }

    @JsonProperty("questionBanks")
    public List<QuestionBankInfo> getQuestionBanks() {
        return getFilterQuestionBanks();
    }

    @JsonProperty("questionBanks")
    public void setQuestionBanks(List<QuestionBankInfo> questionBanks) {
        setFilterQuestionBanks(questionBanks);
    }

    @JsonProperty("allCourses")
    public List<CourseInfo> getAllCourses() {
        return getFilterCourses();
    }

    @JsonProperty("allCourses")
    public void setAllCourses(List<CourseInfo> allCourses) {
        setFilterCourses(allCourses);
    }
}
