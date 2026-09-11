package com.unitbv.myquiz.thy.pagination;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Builds pagination links while preserving the active filters. */
public final class PaginationUrlBuilder {

    private PaginationUrlBuilder() {
    }

    public static String build(String path, int page, int pageSize, Map<String, ?> filters) {
        return build(path, "page", "pageSize", page, pageSize, filters);
    }

    public static String build(String path, String pageParam, String pageSizeParam,
                               int page, int pageSize, Map<String, ?> filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path)
                .queryParam(pageParam, page)
                .queryParam(pageSizeParam, pageSize);
        if (filters != null) {
            filters.forEach((name, value) -> {
                if (value != null && (!(value instanceof String string) || !string.isBlank())) {
                    builder.queryParam(name, value);
                }
            });
        }
        return builder.build().encode().toUriString();
    }
}
