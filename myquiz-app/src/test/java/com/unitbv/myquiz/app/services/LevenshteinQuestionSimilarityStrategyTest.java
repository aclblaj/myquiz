package com.unitbv.myquiz.app.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevenshteinQuestionSimilarityStrategyTest {

    private final LevenshteinQuestionSimilarityStrategy strategy = new LevenshteinQuestionSimilarityStrategy();

    @Test
    void exposesAlgorithmMetadata() {
        assertEquals("levenshtein", strategy.getAlgorithmName());
        assertEquals(0.90d, strategy.getThreshold());
    }

    @Test
    void identicalTextIsSimilar() {
        assertTrue(strategy.isSimilar("What is a deadlock?", "What is a deadlock?"));
    }

    @Test
    void singleCharacterDifferenceInLongTextStaysAboveThreshold() {
        assertTrue(strategy.isSimilar("abcdefghijklmnopqrst", "abcdefghijklmnopqrsx"));
    }

    @Test
    void completelyDifferentShortTextIsNotSimilar() {
        assertFalse(strategy.isSimilar("cat", "dog"));
    }

    @Test
    void nullOrBlankInputIsNotSimilar() {
        assertFalse(strategy.isSimilar(null, "abc"));
        assertFalse(strategy.isSimilar("abc", null));
        assertFalse(strategy.isSimilar("   ", "abc"));
    }
}
