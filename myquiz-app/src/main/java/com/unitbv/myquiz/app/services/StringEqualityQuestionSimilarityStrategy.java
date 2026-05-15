package com.unitbv.myquiz.app.services;

import org.springframework.stereotype.Component;

/**
 * String equality strategy for question text comparison.
 * This strategy considers two strings as similar only if they are exactly equal (case-insensitive).
 */
@Component
public class StringEqualityQuestionSimilarityStrategy extends AbstractQuestionSimilarityStrategy {

    public StringEqualityQuestionSimilarityStrategy() {
        super("string-equality", 1.0d);
    }

    @Override
    protected double similarity(String left, String right) {
        if (left == null || right == null) {
            return 0.0d;
        }
        return left.equalsIgnoreCase(right) ? 1.0d : 0.0d;
    }
}

