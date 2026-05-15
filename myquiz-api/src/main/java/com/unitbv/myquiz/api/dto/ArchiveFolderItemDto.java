package com.unitbv.myquiz.api.dto;

/**
 * One archive processing result item for folder upload.
 */
public class ArchiveFolderItemDto {
    private int index;
    private int total;
    private String archiveName;
    private String status;
    private String courseName;
    private String questionBankName;
    private int filesProcessed;
    private String message;

    public ArchiveFolderItemDto() {
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getQuestionBankName() {
        return questionBankName;
    }

    public void setQuestionBankName(String questionBankName) {
        this.questionBankName = questionBankName;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public void setFilesProcessed(int filesProcessed) {
        this.filesProcessed = filesProcessed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

