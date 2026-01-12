package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Course;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering Course entities.
 * Provides reusable predicates for filtering courses.
 *
 * @author MyQuiz Team
 * @since December 28, 2025
 */
public class CourseSpecification {

    private static final String COURSE = "course";
    private static final String DESCRIPTION = "description";
    private static final String UNIVERSITY_YEAR = "universityYear";
    private static final String SEMESTER = "semester";
    private static final String STUDY_YEAR = "studyYear";

    // Private constructor to prevent instantiation
    private CourseSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by course ID.
     *
     * @param id the course ID
     * @return Specification for filtering by ID
     */
    public static Specification<Course> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get("id"), id);
        };
    }

    /**
     * Filter by course name (case-insensitive, contains).
     *
     * @param courseName the course name
     * @return Specification for filtering by course name
     */
    public static Specification<Course> hasCourseName(String courseName) {
        return (root, query, cb) -> {
            if (courseName == null || courseName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(COURSE)), "%" + courseName.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact course name (case-insensitive).
     *
     * @param courseName the course name
     * @return Specification for filtering by exact course name
     */
    public static Specification<Course> hasCourseNameExact(String courseName) {
        return (root, query, cb) -> {
            if (courseName == null || courseName.isEmpty()) return cb.conjunction();
            return cb.equal(cb.lower(root.get(COURSE)), courseName.toLowerCase());
        };
    }

    /**
     * Filter by description (case-insensitive, contains).
     *
     * @param description the course description
     * @return Specification for filtering by description
     */
    public static Specification<Course> hasDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(DESCRIPTION)), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter by university year.
     *
     * @param universityYear the university year
     * @return Specification for filtering by university year
     */
    public static Specification<Course> hasUniversityYear(String universityYear) {
        return (root, query, cb) -> {
            if (universityYear == null || universityYear.isEmpty()) return cb.conjunction();
            return cb.equal(root.get(UNIVERSITY_YEAR), universityYear);
        };
    }

    /**
     * Filter by semester.
     *
     * @param semester the semester
     * @return Specification for filtering by semester
     */
    public static Specification<Course> hasSemester(String semester) {
        return (root, query, cb) -> {
            if (semester == null || semester.isEmpty()) return cb.conjunction();
            return cb.equal(root.get(SEMESTER), semester);
        };
    }

    /**
     * Filter by study year.
     *
     * @param studyYear the study year
     * @return Specification for filtering by study year
     */
    public static Specification<Course> hasStudyYear(String studyYear) {
        return (root, query, cb) -> {
            if (studyYear == null || studyYear.isEmpty()) return cb.conjunction();
            return cb.equal(root.get(STUDY_YEAR), studyYear);
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param courseName the course name (optional)
     * @param universityYear the university year (optional)
     * @param semester the semester (optional)
     * @param studyYear the study year (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<Course> byFilters(String courseName, String universityYear, String semester, String studyYear) {
        return Specification
                .where(hasCourseName(courseName))
                .and(hasUniversityYear(universityYear))
                .and(hasSemester(semester))
                .and(hasStudyYear(studyYear));
    }

    /**
     * Find course by exact name.
     *
     * @param courseName the course name
     * @return Specification for finding by exact course name
     */
    public static Specification<Course> byCourseName(String courseName) {
        return hasCourseNameExact(courseName);
    }

    /**
     * Find courses by university year and semester.
     *
     * @param universityYear the university year
     * @param semester the semester
     * @return Specification for finding by year and semester
     */
    public static Specification<Course> byYearAndSemester(String universityYear, String semester) {
        return Specification
                .where(hasUniversityYear(universityYear))
                .and(hasSemester(semester));
    }
}

