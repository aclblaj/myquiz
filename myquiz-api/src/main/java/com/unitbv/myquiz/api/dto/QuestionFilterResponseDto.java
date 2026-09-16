package com.unitbv.myquiz.api.dto;

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

    public List<AuthorInfo> getAuthors() {
        return getFilterAuthors();
    }

    public void setAuthors(List<AuthorInfo> authors) {
        setFilterAuthors(authors);
    }

    public List<QuestionBankInfo> getQuestionBanks() {
        return getFilterQuestionBanks();
    }

    public void setQuestionBanks(List<QuestionBankInfo> questionBanks) {
        setFilterQuestionBanks(questionBanks);
    }

    public List<CourseInfo> getAllCourses() {
        return getFilterCourses();
    }

    public void setAllCourses(List<CourseInfo> allCourses) {
        setFilterCourses(allCourses);
    }
}
