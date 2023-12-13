package com.unitbv.myquiz.entities;

public enum QuestionType {
    UNKNOWN(0),
    MULTICHOICE(1);

    final int value;

    QuestionType(int value) {
        this.value = value;
    }

    public static QuestionType fromInteger(Integer value) {
        if (value != null) {
            for (QuestionType questionType : QuestionType.values()) {
                if (questionType.getValue().equals(value)) {
                    return questionType;
                }
            }
        }
        return QuestionType.UNKNOWN;
    }

    public Integer getValue() {
        return value;
    }

}
