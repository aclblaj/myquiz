package com.unitbv.myquiz.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum StudyYear {
    Y2020_2021("2020-2021"),
    Y2021_2022("2021-2022"),
    Y2022_2023("2022-2023"),
    Y2024_2025("2024-2025"),
    Y2025_2026("2025-2026"),
    Y2026_2027("2026-2027");

    private final String value;

    StudyYear(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StudyYear fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(item -> item.value.equals(value) || item.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported study year: " + value));
    }
}

