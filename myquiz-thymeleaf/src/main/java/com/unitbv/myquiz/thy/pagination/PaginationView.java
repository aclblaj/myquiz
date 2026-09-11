package com.unitbv.myquiz.thy.pagination;

import java.util.Map;

/** Common view model consumed by the Thymeleaf pagination fragment. */
public record PaginationView(
        int page,
        int pageSize,
        int totalPages,
        long totalElements,
        String firstUrl,
        String previousUrl,
        String nextUrl,
        String lastUrl
) {

    public static PaginationView of(String path, int page, int pageSize, int totalPages, long totalElements,
                                    Map<String, ?> filters) {
        return of(path, "page", "pageSize", page, pageSize, totalPages, totalElements, filters);
    }

    public static PaginationView of(String path, String pageParam, String pageSizeParam,
                                    int page, int pageSize, int totalPages, long totalElements,
                                    Map<String, ?> filters) {
        int effectiveTotalPages = Math.max(totalPages, 0);
        int effectivePage = effectiveTotalPages == 0
                ? 1
                : Math.max(1, Math.min(page, effectiveTotalPages));
        return new PaginationView(
                effectivePage,
                pageSize,
                effectiveTotalPages,
                Math.max(totalElements, 0),
                PaginationUrlBuilder.build(path, pageParam, pageSizeParam, 1, pageSize, filters),
                PaginationUrlBuilder.build(path, pageParam, pageSizeParam, Math.max(1, effectivePage - 1), pageSize, filters),
                PaginationUrlBuilder.build(path, pageParam, pageSizeParam, Math.min(Math.max(1, effectiveTotalPages), effectivePage + 1), pageSize, filters),
                PaginationUrlBuilder.build(path, pageParam, pageSizeParam, Math.max(1, effectiveTotalPages), pageSize, filters)
        );
    }

    public boolean hasPages() {
        return totalPages > 1;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }
}
