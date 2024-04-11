package com.unitbv.myquiz.util;

public class InputTemplate {
    TemplateType templateType;

    public InputTemplate(TemplateType templateType) {
        this.templateType = templateType;
    }

    private int position_2023_NO = 0;
    private int position_2023_TITLE = 1;
    private int position_2023_TEXT = 2;
    private int position_2023_PR1 = 3;
    private int position_2023_RESPONSE1 = 4;
    private int position_2023_PR2 = 5;
    private int position_2023_RESPONSE2 = 6;
    private int position_2023_PR3 = 7;
    private int position_2023_RESPONSE3 = 8;
    private int position_2023_PR4 = 9;
    private int position_2023_RESPONSE4 = 10;
    private int position_2023_PRTrue = 3;
    private int position_2023_PRFalse = 4;

    private int position_2024_NO = 0;
    private int position_2024_COURSE = 1;
    private int position_2024_TITLE = 2;
    private int position_2024_TEXT = 3;
    private int position_2024_PR1 = 4;
    private int position_2024_RESPONSE1 = 5;
    private int position_2024_PR2 = 6;
    private int position_2024_RESPONSE2 = 7;
    private int position_2024_PR3 = 8;
    private int position_2024_RESPONSE3 = 9;
    private int position_2024_PR4 = 10;
    private int position_2024_RESPONSE4 = 11;
    private int position_2024_PRTrue = 4;
    private int position_2024_PRFalse = 5;


    public int getPositionNO() {
        switch (templateType) {
            case Template2024:
                return position_2024_NO;
            default:
                return position_2023_NO;
        }
    }

    public int getPositionTitle() {
        switch (templateType) {
            case Template2024:
                return position_2024_TITLE;
            default:
                return position_2023_TITLE;
        }
    }

    public int getPositionText() {
        switch (templateType) {
            case Template2024:
                return position_2024_TEXT;
            default:
                return position_2023_TEXT;
        }
    }
    public int getPositionPR1() {
        switch (templateType) {
            case Template2024:
                return position_2024_PR1;
            default:
                return position_2023_PR1;
        }
    }

    public int getPositionResponse1() {
        switch (templateType) {
            case Template2024:
                return position_2024_RESPONSE1;
            default:
                return position_2023_RESPONSE1;
        }
    }

    public int getPositionPR2() {
        switch (templateType) {
            case Template2024:
                return position_2024_PR2;
            default:
                return position_2023_PR2;
        }
    }

    public int getPositionResponse2() {
        switch (templateType) {
            case Template2024:
                return position_2024_RESPONSE2;
            default:
                return position_2023_RESPONSE2;
        }
    }

    public int getPositionPR3() {
        switch (templateType) {
            case Template2024:
                return position_2024_PR3;
            default:
                return position_2023_PR3;
        }
    }

    public int getPositionResponse3() {
        switch (templateType) {
            case Template2024:
                return position_2024_RESPONSE3;
            default:
                return position_2023_RESPONSE3;
        }
    }

    public int getPositionPR4() {
        switch (templateType) {
            case Template2024:
                return position_2024_PR4;
            default:
                return position_2023_PR4;
        }
    }

    public int getPositionResponse4() {
        switch (templateType) {
            case Template2024:
                return position_2024_RESPONSE4;
            default:
                return position_2023_RESPONSE4;
        }
    }

    public int getPositionPRTrue() {
        switch (templateType) {
            case Template2024:
                return position_2024_PRTrue;
            default:
                return position_2023_PRTrue;
        }
    }

    public int getPositionPRFalse() {
        switch (templateType) {
            case Template2024:
                return position_2024_PRFalse;
            default:
                return position_2023_PRFalse;
        }
    }

    public int getPositionCourse() {
        switch (templateType) {
            case Template2024:
                return position_2024_COURSE;
            default:
                return -1;
        }
    }
}
