package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourseDto {
    private Long id;
    private String course;
    private String description;
    private String universityYear;
    private String semester;
    @JsonProperty("study_year")
    private String studyYear;

    public CourseDto() {
    }

    public CourseDto(Long id, String course) {
        this.id = id;
        this.course = course;
    }

    public CourseDto(Long id, String course, String description, String universityYear, String semester, String studyYear) {
        this.id = id;
        this.course = course;
        this.description = description;
        this.universityYear = universityYear;
        this.semester = semester;
        this.studyYear = studyYear;

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
    public String getStudyYear() {
        return studyYear;
    }
    public void setStudyYear(String studyYear) {
        this.studyYear = studyYear;
    }

}