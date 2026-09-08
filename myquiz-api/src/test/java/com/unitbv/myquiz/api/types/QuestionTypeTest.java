package com.unitbv.myquiz.api.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionTypeTest {

    @Test
    void fromIntegerReturnsMatchingType() {
        assertEquals(QuestionType.UNKNOWN, QuestionType.fromInteger(0));
        assertEquals(QuestionType.MULTICHOICE, QuestionType.fromInteger(1));
        assertEquals(QuestionType.TRUEFALSE, QuestionType.fromInteger(2));
    }

    @Test
    void fromIntegerReturnsUnknownForNull() {
        assertEquals(QuestionType.UNKNOWN, QuestionType.fromInteger(null));
    }

    @Test
    void fromIntegerReturnsUnknownForUnmappedValue() {
        assertEquals(QuestionType.UNKNOWN, QuestionType.fromInteger(99));
    }

    @Test
    void getValueReturnsUnderlyingCode() {
        assertEquals(0, QuestionType.UNKNOWN.getValue());
        assertEquals(1, QuestionType.MULTICHOICE.getValue());
        assertEquals(2, QuestionType.TRUEFALSE.getValue());
    }

    @Test
    void getAcronymReturnsShortCode() {
        assertEquals("UN", QuestionType.UNKNOWN.getAcronym());
        assertEquals("MC", QuestionType.MULTICHOICE.getAcronym());
        assertEquals("TF", QuestionType.TRUEFALSE.getAcronym());
    }

    @Test
    void getTypeAsStringReturnsEnumName() {
        assertEquals("MULTICHOICE", QuestionType.getTypeAsString(QuestionType.MULTICHOICE));
    }

    @Test
    void getTypeAsStringFromIntegerResolvesNameFromCode() {
        assertEquals("TRUEFALSE", QuestionType.getTypeAsStringFromInteger(2));
        assertEquals("UNKNOWN", QuestionType.getTypeAsStringFromInteger(null));
    }

    @Test
    void getAllTypesAsStringArrayContainsEveryType() {
        assertArrayEquals(
                new String[]{"UNKNOWN", "MULTICHOICE", "TRUEFALSE"},
                QuestionType.getAllTypesAsStringArray());
    }
}
