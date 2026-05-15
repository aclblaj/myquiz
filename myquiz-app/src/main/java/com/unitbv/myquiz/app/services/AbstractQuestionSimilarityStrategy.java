package com.unitbv.myquiz.app.services;

/**
 * Base strategy for question text similarity metrics.
 */
public abstract class AbstractQuestionSimilarityStrategy {

    private final String algorithmName;
    private final double threshold;

    protected AbstractQuestionSimilarityStrategy(String algorithmName, double threshold) {
        this.algorithmName = algorithmName;
        this.threshold = threshold;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isSimilar(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.isBlank() || right.isBlank()) {
            return false;
        }
        return similarity(left, right) >= threshold;
    }

    protected abstract double similarity(String left, String right);
}

