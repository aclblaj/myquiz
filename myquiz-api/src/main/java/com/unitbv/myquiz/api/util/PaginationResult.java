package com.unitbv.myquiz.api.util;

import java.util.List;

/**
 * Framework-neutral result of a paginated operation.
 *
 * @param items         items for the effective page
 * @param page          effective 1-based page number
 * @param pageSize      effective page size
 * @param totalElements total number of matching items
 * @param totalPages    number of pages; zero when there are no items
 */
public record PaginationResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public PaginationResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
