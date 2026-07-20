package com.unitbv.myquiz.app.upload.application.support;

public enum ArchiveFolderItemStatus {
    SKIPPED,
    PROCESSED,
    FAILED;

    public String value() {
        return name();
    }
}

