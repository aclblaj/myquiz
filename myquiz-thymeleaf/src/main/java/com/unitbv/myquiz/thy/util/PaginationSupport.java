package com.unitbv.myquiz.thy.util;

import com.unitbv.myquiz.api.settings.ControllerSettings;

/**
 * Shared normalization logic for page and pageSize query parameters.
 */
public final class PaginationSupport {

    private PaginationSupport() {
    }

    public static PaginationParams normalize(Integer page, Integer pageSize) {
        int defaultPage = parseOrDefault(ControllerSettings.DEFAULT_PAGE, 1);
        int defaultPageSize = parseOrDefault(ControllerSettings.DEFAULT_PAGE_SIZE, ControllerSettings.PAGE_SIZE);

        int normalizedPage = page == null ? defaultPage : page;
        int normalizedPageSize = pageSize == null ? defaultPageSize : pageSize;

        if (normalizedPage < 1) {
            normalizedPage = defaultPage;
        }
        if (normalizedPageSize < 1) {
            normalizedPageSize = defaultPageSize;
        }

        return new PaginationParams(normalizedPage, normalizedPageSize);
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

