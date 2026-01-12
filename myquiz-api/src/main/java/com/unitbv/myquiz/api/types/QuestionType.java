package com.unitbv.myquiz.api.types;

public enum QuestionType {
    UNKNOWN(0),
    MULTICHOICE(1),
    TRUEFALSE(2);
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
