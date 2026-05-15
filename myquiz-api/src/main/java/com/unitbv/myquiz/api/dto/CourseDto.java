package com.unitbv.myquiz.api.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseDto {
    private Long id;
    private String course;
    private String description;
    private String universityYear;
    private String semester;
    private int questionBankCount;
    private List<CourseSourceDto> sources = new ArrayList<>();

    public CourseDto() {
    }

    public CourseDto(Long id, String course) {
        this.id = id;
        this.course = course;
    }

    public CourseDto(Long id, String course, String description, String universityYear, String semester) {
        this.id = id;
        this.course = course;
        this.description = description;
        this.universityYear = universityYear;
        this.semester = semester;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUniversityYear() {
        return universityYear;
    }

    public void setUniversityYear(String universityYear) {
        this.universityYear = universityYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getQuestionBankCount() {
        return questionBankCount;
    }

    public void setQuestionBankCount(int questionBankCount) {
        this.questionBankCount = questionBankCount;
    }

    public List<CourseSourceDto> getSources() {
        return sources;
    }

    public void setSources(List<CourseSourceDto> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
    }

}
