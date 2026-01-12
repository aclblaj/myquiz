package com.unitbv.myquiz.app.criteria;

/**
 * Filter criteria for Author queries.
 * This class encapsulates the filtering parameters used when searching for authors.
 */
public class AuthorFilterCriteria {
    private String course;
    private Long authorId;

    /**
     * Default constructor.
     */
    public AuthorFilterCriteria() {
    }

    /**
     * Constructor with all filter parameters.
     *
     * @param course The course name to filter by (case-insensitive)
     * @param authorId The author ID to filter by
     */
    public AuthorFilterCriteria(String course, Long authorId) {
        this.course = course;
        this.authorId = authorId;
    }

    /**
     * Gets the course filter.
     *
     * @return The course name
     */
    public String getCourse() {
        return course;
    }

    /**
     * Sets the course filter.
     *
     * @param course The course name
     */
    public void setCourse(String course) {
        this.course = course;
    }

    /**
     * Gets the author ID filter.
     *
     * @return The author ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * Sets the author ID filter.
     *
     * @param authorId The author ID
     */
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    /**
     * Checks if the course filter is set.
     *
     * @return true if course is not null and not empty
     */
    public boolean hasCourse() {
        return course != null && !course.isEmpty();
    }

    /**
     * Checks if the author ID filter is set.
     *
     * @return true if authorId is not null
     */
    public boolean hasAuthorId() {
        return authorId != null;
    }

    /**
     * Checks if any filter is set.
     *
     * @return true if at least one filter is active
     */
    public boolean hasAnyFilter() {
        return hasCourse() || hasAuthorId();
    }

    @Override
    public String toString() {
        return "AuthorFilterCriteria{" +
                "course='" + course + '\'' +
                ", authorId=" + authorId +
                '}';
    }
}

