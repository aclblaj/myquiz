package com.unitbv.myquiz.api.dto;

/**
 * Result object for archive upload processing.
 */
public record ArchiveUploadResult(int filesProcessed, String questionBankName, Long questionBankId) {

    public String toMessage() {
        return "Imported " + filesProcessed + " files successfully for questionBank '" + questionBankName + "'";
    }
}

