package com.unitbv.myquiz.app.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaroWinklerQuestionSimilarityStrategyTest {

    private final JaroWinklerQuestionSimilarityStrategy strategy = new JaroWinklerQuestionSimilarityStrategy();

    @Test
    void exposesAlgorithmMetadata() {
        assertEquals("jaro-winkler", strategy.getAlgorithmName());
        assertEquals(0.93d, strategy.getThreshold());
    }

    @Test
    void identicalTextIsSimilar() {
        assertTrue(strategy.isSimilar("Explain mutual exclusion", "Explain mutual exclusion"));
    }

    @Test
    void transposedNearMatchStaysAboveThreshold() {
        assertTrue(strategy.isSimilar("MARTHA", "MARHTA"));
    }

    @Test
    void unrelatedTextIsNotSimilar() {
        assertFalse(strategy.isSimilar("abcdef", "uvwxyz"));
    }

    @Test
    void nullOrBlankInputIsNotSimilar() {
        assertFalse(strategy.isSimilar(null, "abc"));
        assertFalse(strategy.isSimilar("abc", null));
        assertFalse(strategy.isSimilar("abc", "   "));
    }
}
