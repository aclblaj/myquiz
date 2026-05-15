package com.unitbv.myquiz.api.types;

/**
 * Enumeration of available duplicate comparison strategies.
 */
public enum DuplicateComparisonStrategy {
    LEVENSHTEIN("levenshtein", "Levenshtein Distance", "Compares text similarity allowing for small variations"),
    JARO_WINKLER("jaro-winkler", "Jaro-Winkler", "Advanced string similarity with prefix weighting"),
    STRING_EQUALITY("string-equality", "Exact Match", "Requires exact text match (case-insensitive)");

    private final String algorithmName;
    private final String displayName;
    private final String description;

    DuplicateComparisonStrategy(String algorithmName, String displayName, String description) {
        this.algorithmName = algorithmName;
        this.displayName = displayName;
        this.description = description;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static DuplicateComparisonStrategy fromAlgorithmName(String algorithmName) {
        if (algorithmName == null) {
            return LEVENSHTEIN;
        }
        for (DuplicateComparisonStrategy strategy : values()) {
            if (strategy.algorithmName.equalsIgnoreCase(algorithmName)) {
                return strategy;
            }
        }
        return LEVENSHTEIN;
    }
}

