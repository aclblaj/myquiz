package com.unitbv.myquiz.api.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudyYearTest {

    @Test
    void getValueReturnsDisplayString() {
        assertEquals("2020-2021", StudyYear.Y2020_2021.getValue());
        assertEquals("2025-2026", StudyYear.Y2025_2026.getValue());
    }

    @Test
    void fromValueResolvesByDisplayString() {
        assertEquals(StudyYear.Y2022_2023, StudyYear.fromValue("2022-2023"));
    }

    @Test
    void fromValueResolvesByEnumNameIgnoringCase() {
        assertEquals(StudyYear.Y2026_2027, StudyYear.fromValue("y2026_2027"));
    }

    @Test
    void fromValueReturnsNullForNullOrBlank() {
        assertNull(StudyYear.fromValue(null));
        assertNull(StudyYear.fromValue("   "));
    }

    @Test
    void fromValueThrowsForUnsupportedValue() {
        assertThrows(IllegalArgumentException.class, () -> StudyYear.fromValue("1999-2000"));
    }
}
