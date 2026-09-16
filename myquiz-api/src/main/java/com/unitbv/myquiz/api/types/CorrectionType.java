package com.unitbv.myquiz.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Supported AI operations for question correction. */
public enum CorrectionType {
    GRAMMAR("grammar"),
    IMPROVE("improve"),
    ALTERNATIVES("alternatives"),
    EXPLAIN("explain"),
    UNKNOWN("unknown");

    private final String value;

    CorrectionType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CorrectionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (CorrectionType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim()) || type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
