package com.unitbv.myquiz.api.types;

/**
 * Enum containing default courses that can be auto-added to the system.
 * These are predefined courses commonly used in the curriculum.
 */
public enum DefaultCourse {
    SISTEME_DE_OPERARE("Sisteme de operare"),
    RETELE_DE_CALCULATOARE("Retele de calculatoare"),
    CYBERSECURITY("Cybersecurity"),
    SECURITATEA_SISTEMELOR_INFORMATICE("Securitatea sistemelor informatice"),
    BAZE_DE_DATE("Baze de date"),
    BAZE_DE_DATE_DISTRIBUITE("Baze de date dristribuite"),
    APLICATII_MOBILE("Aplicatii mobile"),
    ALGORITMI_IN_RETEA("Algoritmi in retea");

    private final String courseName;

    DefaultCourse(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseName() {
        return courseName;
    }

    public static DefaultCourse[] getAllCourses() {
        return values();
    }
}

