package com.unitbv.myquiz.api.util;

import com.unitbv.myquiz.api.settings.ControllerSettings;

import java.util.List;

/**
 * Shared pagination normalization logic for API consumers.
 */
public final class PaginationSupport {
    private PaginationSupport() {
    }

    public static PaginationParams normalize(Integer page, Integer pageSize) {
        int defaultPage = parseOrDefault(ControllerSettings.DEFAULT_PAGE, 1);
        int defaultPageSize = parseOrDefault(ControllerSettings.DEFAULT_PAGE_SIZE, ControllerSettings.PAGE_SIZE);

        int normalizedPage = page == null || page < 1 ? defaultPage : page;
        int requestedPageSize = pageSize == null || pageSize < 1 ? defaultPageSize : pageSize;
        int normalizedPageSize = Math.min(requestedPageSize, ControllerSettings.MAX_PAGE_SIZE);

        return new PaginationParams(normalizedPage, normalizedPageSize);
    }

    /**
     * Applies the common pagination rules to an in-memory collection.
     * Database-backed callers should use the normalized values to build a
     * repository Pageable instead of loading the complete collection.
     */
    public static <T> PaginationResult<T> paginate(List<T> items, Integer page, Integer pageSize) {
        return paginate(items, normalize(page, pageSize));
    }

    public static <T> PaginationResult<T> paginate(List<T> items, PaginationParams pagination) {
        List<T> source = items == null ? List.of() : items;
        PaginationParams effective = pagination == null
                ? normalize(null, null)
                : normalize(pagination.page(), pagination.pageSize());
        long totalElements = source.size();
        int totalPages = totalPages(totalElements, effective.pageSize());
        int effectivePage = totalPages == 0 ? 1 : Math.min(effective.page(), totalPages);
        long offset = (long) (effectivePage - 1) * effective.pageSize();
        int fromIndex = (int) Math.min(offset, totalElements);
        int toIndex = (int) Math.min(offset + effective.pageSize(), totalElements);

        return new PaginationResult<>(source.subList(fromIndex, toIndex), effectivePage, effective.pageSize(), totalElements, totalPages);
    }

    public static int totalPages(long totalElements, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        if (totalElements <= 0) {
            return 0;
        }
        return (int) ((totalElements + pageSize - 1L) / pageSize);
    }

    private static int parseOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
