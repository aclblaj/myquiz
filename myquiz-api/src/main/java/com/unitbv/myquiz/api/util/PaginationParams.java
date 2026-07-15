package com.unitbv.myquiz.api.util;

/**
 * Normalized pagination parameters.
 *
 * @param page     1-based page number
 * @param pageSize number of items per page
 */
public record PaginationParams(int page, int pageSize) {
}
