package com.unitbv.myquiz.app.util;

import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

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

    public static Pageable toPageable(Integer page, Integer pageSize, String sortField, String sortDirection,
                                      String... tieBreakers) {
        return toPageable(PaginationSupport.normalize(page, pageSize), sortField, sortDirection, tieBreakers);
    }

    public static Pageable toPageable(PaginationParams pagination) {
        return toPageable(pagination, null, null);
    }

    public static Pageable toPageable(PaginationParams pagination, String sortField, String sortDirection) {
        return toPageable(pagination, sortField, sortDirection, new String[0]);
    }

    /** Creates a deterministic order by appending ascending tie-breaker fields. */
    public static Pageable toPageable(PaginationParams pagination, String sortField, String sortDirection,
                                      String... tieBreakers) {
        PaginationParams effective = pagination == null
                ? PaginationSupport.normalize(null, null)
                : PaginationSupport.normalize(pagination.page(), pagination.pageSize());
        if (sortField == null || sortField.isBlank()) {
            return PageRequest.of(effective.page() - 1, effective.pageSize());
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String normalizedSortField = sortField.trim();
        Sort sort = "crtNo".equals(normalizedSortField)
                ? JpaSort.unsafe(Sort.Direction.ASC, "CASE WHEN crtNo = 0 THEN 1 ELSE 0 END")
                .and(Sort.by(direction, normalizedSortField))
                : Sort.by(direction, normalizedSortField);
        if (tieBreakers != null) {
            for (String tieBreaker : tieBreakers) {
                if (tieBreaker != null && !tieBreaker.isBlank() && !normalizedSortField.equals(tieBreaker.trim())) {
                    sort = sort.and(Sort.by(Sort.Direction.ASC, tieBreaker.trim()));
                }
            }
        }
        return PageRequest.of(effective.page() - 1, effective.pageSize(), sort);
    }
}
