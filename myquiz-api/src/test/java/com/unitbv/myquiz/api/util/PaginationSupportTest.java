package com.unitbv.myquiz.api.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationSupportTest {

    @Test
    void normalizesInvalidValuesAndCapsPageSize() {
        PaginationParams result = PaginationSupport.normalize(0, 10_000);

        assertEquals(1, result.page());
        assertEquals(100, result.pageSize());
    }

    @Test
    void clampsOutOfRangePageAndSlicesCollection() {
        PaginationResult<Integer> result = PaginationSupport.paginate(List.of(1, 2, 3), 9, 2);

        assertEquals(List.of(3), result.items());
        assertEquals(2, result.page());
        assertEquals(2, result.totalPages());
        assertEquals(3, result.totalElements());
    }

    @Test
    void representsEmptyCollectionConsistently() {
        PaginationResult<Integer> result = PaginationSupport.paginate(List.of(), 5, 2);

        assertEquals(List.of(), result.items());
        assertEquals(1, result.page());
        assertEquals(0, result.totalPages());
        assertEquals(0, result.totalElements());
    }
}
