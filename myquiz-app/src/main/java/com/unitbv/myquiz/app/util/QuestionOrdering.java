package com.unitbv.myquiz.app.util;

import com.unitbv.myquiz.app.entities.Question;

import java.util.Comparator;

/** Shared ordering for question lists that can be paginated by a consumer. */
public final class QuestionOrdering {

    private QuestionOrdering() {
    }

    /** Orders numbered questions first and makes equal row numbers deterministic. */
    public static Comparator<Question> byCrtNoThenId() {
        return Comparator.comparingInt(QuestionOrdering::normalizedCrtNo)
                .thenComparing(
                        question -> question == null ? null : question.getId(),
                        Comparator.nullsLast(Long::compareTo)
                );
    }

    private static int normalizedCrtNo(Question question) {
        if (question == null || question.getCrtNo() == 0) {
            return Integer.MAX_VALUE;
        }
        return question.getCrtNo();
    }
}
