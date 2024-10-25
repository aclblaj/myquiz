package com.unitbv.myquiz.util;

public enum TemplateType {
    Template2023("template2023", 2023L),
    Template2024("template2024", 2024L);

    private final String type;
    private final Long year;

    TemplateType(String type, Long year) {
        this.type = type;
        this.year = year;
    }

    public String getType() {
        return type;
    }

    public Long getYear() {
        return year;
    }

    public static TemplateType fromString(String text) {
        for (TemplateType b : TemplateType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public static TemplateType fromType(String type) {
        for (TemplateType b : TemplateType.values()) {
            if (b.type.equalsIgnoreCase(type)) {
                return b;
            }
        }
        return null;
    }

    public static TemplateType fromType(int type) {
        for (TemplateType b : TemplateType.values()) {
            if (Integer.parseInt(b.type) == type) {
                return b;
            }
        }
        return null;
    }

}
