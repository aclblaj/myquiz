package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("authorList")
    public List<AuthorInfo> getAuthorList() {
        return getFilterAuthors();
    }

    @JsonProperty("authorList")
    public void setAuthorList(List<AuthorInfo> authorList) {
        setFilterAuthors(authorList);
    }

    @JsonProperty("authorOptions")
    public List<AuthorInfo> getAuthorOptions() {
        return getFilterAuthors();
    }

    @JsonProperty("authorOptions")
    public void setAuthorOptions(List<AuthorInfo> authorOptions) {
        setFilterAuthors(authorOptions);
    }
}
