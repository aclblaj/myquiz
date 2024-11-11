package com.unitbv.myquiz.dto;

public class CourseDto {
    Long id;
    String course;
    String description;
    String university_year;
    String semester;
    String study_year;

    public CourseDto(Long id, String course, String description,
                     String university_year, String semester, String study_year) {
        this.id = id;
        this.course = course;
        this.description = description;
        this.university_year = university_year;
        this.semester = semester;
        this.study_year = study_year;
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

    public String getUniversity_year() {
        return university_year;
    }

    public void setUniversity_year(String university_year) {
        this.university_year = university_year;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStudy_year() {
        return study_year;
    }

    public void setStudy_year(String study_year) {
        this.study_year = study_year;
    }
}
