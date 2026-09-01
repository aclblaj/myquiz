package com.unitbv.myquiz.api.util;

import com.unitbv.myquiz.api.settings.ControllerSettings;

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
        int normalizedPageSize = pageSize == null || pageSize < 1 ? defaultPageSize : pageSize;

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
