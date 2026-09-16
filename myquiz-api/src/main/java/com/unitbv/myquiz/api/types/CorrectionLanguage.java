package com.unitbv.myquiz.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Languages supported by the AI correction prompts. */
public enum CorrectionLanguage {
    ROMANIAN("ro"),
    ENGLISH("en");

    private final String value;

    CorrectionLanguage(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CorrectionLanguage fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (CorrectionLanguage language : values()) {
            if (language.value.equalsIgnoreCase(value.trim()) || language.name().equalsIgnoreCase(value.trim())) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unsupported correction language: " + value);
    }

    @JsonValue
    public String value() {
        return value;
    }
}
