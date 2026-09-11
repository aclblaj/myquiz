package com.unitbv.myquiz.thy.pagination;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationViewTest {

    @Test
    void clampsPageAndPreservesFiltersInEveryLink() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("courseId", 7L);
        filters.put("author", "Ana Pop");

        PaginationView pagination = PaginationView.of("/questions", 99, 20, 3, 42, filters);

        assertEquals(3, pagination.page());
        assertTrue(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
        assertTrue(pagination.firstUrl().contains("courseId=7"));
        assertTrue(pagination.firstUrl().contains("author=Ana%20Pop"));
        assertTrue(pagination.lastUrl().contains("page=3"));
    }

    @Test
    void usesPageOneForEmptyResults() {
        PaginationView pagination = PaginationView.of("/courses", 5, 10, 0, 0, Map.of());

        assertEquals(1, pagination.page());
        assertEquals(0, pagination.totalPages());
        assertFalse(pagination.hasPages());
        assertFalse(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
}
