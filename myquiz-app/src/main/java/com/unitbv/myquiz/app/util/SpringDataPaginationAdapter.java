package com.unitbv.myquiz.app.util;

import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Translates the framework-neutral pagination contract into Spring Data.
 * Spring Data remains an application implementation detail and does not leak
 * into the shared API module.
 */
public final class SpringDataPaginationAdapter {

    private SpringDataPaginationAdapter() {
    }

    public static Pageable toPageable(Integer page, Integer pageSize, String sortField, String sortDirection) {
        return toPageable(PaginationSupport.normalize(page, pageSize), sortField, sortDirection);
    }

    public static Pageable toPageable(PaginationParams pagination) {
        return toPageable(pagination, null, null);
    }

    public static Pageable toPageable(PaginationParams pagination, String sortField, String sortDirection) {
        PaginationParams effective = pagination == null
                ? PaginationSupport.normalize(null, null)
                : PaginationSupport.normalize(pagination.page(), pagination.pageSize());
        if (sortField == null || sortField.isBlank()) {
            return PageRequest.of(effective.page() - 1, effective.pageSize());
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField.trim());
        return PageRequest.of(effective.page() - 1, effective.pageSize(), sort);
    }
}
