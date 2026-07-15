package com.unitbv.myquiz.api.util;

import com.unitbv.myquiz.api.settings.ControllerSettings;

/**
 * Shared pagination normalization logic for API consumers.
 */
public final class PaginationSupport {
    private PaginationSupport() {
    }

    public static PaginationParams normalize(Integer page, Integer pageSize) {
        int safePage = page != null && page > 0
                ? page
                : Integer.parseInt(ControllerSettings.DEFAULT_PAGE);
        int safePageSize = pageSize != null && pageSize > 0
                ? pageSize
                : Integer.parseInt(ControllerSettings.DEFAULT_PAGE_SIZE);
        return new PaginationParams(safePage, safePageSize);
    }
}
