package com.unitbv.myquiz.app.services;

import org.springframework.stereotype.Component;

@Component
public class LevenshteinQuestionSimilarityStrategy extends AbstractQuestionSimilarityStrategy {

    public LevenshteinQuestionSimilarityStrategy() {
        super("levenshtein", 0.90d);
    }

    @Override
    protected double similarity(String left, String right) {
        int maxLen = Math.max(left.length(), right.length());
        if (maxLen == 0) {
            return 1.0d;
        }
        int distance = levenshteinDistance(left, right);
        return 1.0d - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            char leftChar = left.charAt(i - 1);
            for (int j = 1; j <= right.length(); j++) {
                int substitutionCost = leftChar == right.charAt(j - 1) ? 0 : 1;
                int delete = previous[j] + 1;
                int insert = current[j - 1] + 1;
                int substitute = previous[j - 1] + substitutionCost;
                current[j] = Math.min(Math.min(delete, insert), substitute);
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[right.length()];
    }
}

