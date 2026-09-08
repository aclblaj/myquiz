package com.unitbv.myquiz.app.services;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyUtilTest {

    @Test
    void isDuplicateValidationErrorReturnsFalseForNullOrBlank() {
        assertFalse(MyUtil.isDuplicateValidationError(null));
        assertFalse(MyUtil.isDuplicateValidationError("   "));
    }

    @Test
    void isDuplicateValidationErrorDetectsDuplicatePrefixes() {
        assertTrue(MyUtil.isDuplicateValidationError(
                MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS + " [duplicate title]"));
        assertTrue(MyUtil.isDuplicateValidationError(
                MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS));
    }

    @Test
    void isDuplicateValidationErrorReturnsFalseForOtherErrors() {
        assertFalse(MyUtil.isDuplicateValidationError(MyUtil.MISSING_TITLE));
    }

    @Test
    void getPageableReturnsUnpagedForMinusOne() {
        Pageable pageable = MyUtil.getPageable(-1, 10, null, null);
        assertTrue(pageable.isUnpaged());
    }

    @Test
    void getPageableWithoutSortFieldIsUnsorted() {
        Pageable pageable = MyUtil.getPageable(1, 10, null, null);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(pageable.getSort().isUnsorted());
    }

    @Test
    void getPageableAppliesDescendingSort() {
        Pageable pageable = MyUtil.getPageable(2, 5, "name", "desc");
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        Sort.Order order = pageable.getSort().getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getPageableRejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () -> MyUtil.getPageable(1, 0, null, null));
    }
}
