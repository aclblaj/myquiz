package com.unitbv.myquiz.api.dto;

public class CourseSourceDto {
    private Long questionBankId;
    private String courseQuestionBank;
    private int mcQuestionsCount;
    private int tfQuestionsCount;
    private int totalQuestionsCount;
    private int authorsCount;
    private String archiveImport;

    public CourseSourceDto() {
    }

    public CourseSourceDto(Long questionBankId, String courseQuestionBank, String archiveImport) {
        this.questionBankId = questionBankId;
        this.courseQuestionBank = courseQuestionBank;
        this.archiveImport = archiveImport;
    }

    public CourseSourceDto(Long questionBankId, String courseQuestionBank, int mcQuestionsCount, int tfQuestionsCount, int totalQuestionsCount, int authorsCount, String archiveImport) {
        this.questionBankId = questionBankId;
        this.courseQuestionBank = courseQuestionBank;
        this.mcQuestionsCount = mcQuestionsCount;
        this.tfQuestionsCount = tfQuestionsCount;
        this.totalQuestionsCount = totalQuestionsCount;
        this.authorsCount = authorsCount;
        this.archiveImport = archiveImport;
    }

    public CourseSourceDto(String courseQuestionBank, String archiveImport) {
        this.courseQuestionBank = courseQuestionBank;
        this.archiveImport = archiveImport;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(Long questionBankId) {
        this.questionBankId = questionBankId;
    }

    public String getCourseQuestionBank() {
        return courseQuestionBank;
    }

    public void setCourseQuestionBank(String courseQuestionBank) {
        this.courseQuestionBank = courseQuestionBank;
    }

    public String getArchiveImport() {
        return archiveImport;
    }

    public void setArchiveImport(String archiveImport) {
        this.archiveImport = archiveImport;
    }

    public int getMcQuestionsCount() {
        return mcQuestionsCount;
    }

    public void setMcQuestionsCount(int mcQuestionsCount) {
        this.mcQuestionsCount = mcQuestionsCount;
    }

    public int getTfQuestionsCount() {
        return tfQuestionsCount;
    }

    public void setTfQuestionsCount(int tfQuestionsCount) {
        this.tfQuestionsCount = tfQuestionsCount;
    }

    public int getTotalQuestionsCount() {
        return totalQuestionsCount;
    }

    public void setTotalQuestionsCount(int totalQuestionsCount) {
        this.totalQuestionsCount = totalQuestionsCount;
    }

    public int getAuthorsCount() {
        return authorsCount;
    }

    public void setAuthorsCount(int authorsCount) {
        this.authorsCount = authorsCount;
    }
}

