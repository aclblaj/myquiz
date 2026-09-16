package com.unitbv.myquiz.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Controlled lifecycle values exposed by question errors and duplicate links. */
public enum ResolutionStatus {
    OPEN,
    RESOLVED,
    UNKNOWN;

    @JsonCreator
    public static ResolutionStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
