package com.unitbv.myquiz.util;

public enum TemplateType {
    Template2023("2023"),
    Template2024("2024");

    private final String type;

    TemplateType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
