package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Canonical response DTO for author filtering operations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AuthorFilterResponseDto extends BaseFilterDto {
    private List<AuthorDto> authors;
    private String selectedCourse;
    private Long selectedCourseId;
    private Long selectedQuestionBankId;

    public List<CourseInfo> getCourses() {
        return getFilterCourses();
    }

    public void setCourses(List<CourseInfo> courses) {
        setFilterCourses(courses);
    }

    public List<QuestionBankInfo> getQuestionBanks() {
        return getFilterQuestionBanks();
    }

    public void setQuestionBanks(List<QuestionBankInfo> questionBanks) {
        setFilterQuestionBanks(questionBanks);
    }

    public List<AuthorInfo> getAuthorList() {
        return getFilterAuthors();
    }

    public void setAuthorList(List<AuthorInfo> authorList) {
        setFilterAuthors(authorList);
    }

    public List<AuthorInfo> getAuthorOptions() {
        return getFilterAuthors();
    }

    public void setAuthorOptions(List<AuthorInfo> authorOptions) {
        setFilterAuthors(authorOptions);
    }
}
