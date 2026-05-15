package com.unitbv.myquiz.app.services;

import org.springframework.stereotype.Component;

@Component
public class JaroWinklerQuestionSimilarityStrategy extends AbstractQuestionSimilarityStrategy {

    private static final double PREFIX_SCALING = 0.1d;

    public JaroWinklerQuestionSimilarityStrategy() {
        super("jaro-winkler", 0.93d);
    }

    @Override
    protected double similarity(String left, String right) {
        double jaro = jaroSimilarity(left, right);
        int commonPrefixLength = commonPrefixLength(left, right, 4);
        return jaro + (commonPrefixLength * PREFIX_SCALING * (1.0d - jaro));
    }

    private double jaroSimilarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0d;
        }

        int leftLength = left.length();
        int rightLength = right.length();
        if (leftLength == 0 || rightLength == 0) {
            return 0.0d;
        }

        int matchDistance = Math.max(leftLength, rightLength) / 2 - 1;
        if (matchDistance < 0) {
            matchDistance = 0;
        }

        boolean[] leftMatches = new boolean[leftLength];
        boolean[] rightMatches = new boolean[rightLength];

        int matches = 0;
        for (int i = 0; i < leftLength; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, rightLength);

            for (int j = start; j < end; j++) {
                if (rightMatches[j] || left.charAt(i) != right.charAt(j)) {
                    continue;
                }
                leftMatches[i] = true;
                rightMatches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0d;
        }

        double transpositions = 0;
        int rightIndex = 0;
        for (int i = 0; i < leftLength; i++) {
            if (!leftMatches[i]) {
                continue;
            }
            while (!rightMatches[rightIndex]) {
                rightIndex++;
            }
            if (left.charAt(i) != right.charAt(rightIndex)) {
                transpositions++;
            }
            rightIndex++;
        }

        transpositions /= 2.0d;

        return ((double) matches / leftLength
                + (double) matches / rightLength
                + ((double) matches - transpositions) / matches) / 3.0d;
    }

    private int commonPrefixLength(String left, String right, int maxPrefix) {
        int boundary = Math.min(Math.min(left.length(), right.length()), maxPrefix);
        int index = 0;
        while (index < boundary && left.charAt(index) == right.charAt(index)) {
            index++;
        }
        return index;
    }
}

