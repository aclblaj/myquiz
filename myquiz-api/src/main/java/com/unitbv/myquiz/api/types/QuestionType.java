package com.unitbv.myquiz.api.types;

public enum QuestionType {
    UNKNOWN(0, "UN"),
    MULTICHOICE(1, "MC"),
    TRUEFALSE(2, "TF");

    final int value;
    final String acronym;

    QuestionType(int value, String acronym) {
        this.value = value;
        this.acronym = acronym;
    }

    public String getAcronym() {
        return acronym;
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

    public static String getTypeAsString(QuestionType type) {
        return type.name();
    }

    public static String getTypeAsStringFromInteger(Integer value) {
        QuestionType type = fromInteger(value);
        return type.name();
    }

    public static String[] getAllTypesAsStringArray() {
        QuestionType[] types = QuestionType.values();
        String[] typesAsString = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typesAsString[i] = types[i].name();
        }
        return typesAsString;
    }

}
