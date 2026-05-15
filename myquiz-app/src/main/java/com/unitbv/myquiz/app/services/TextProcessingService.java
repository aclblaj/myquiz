package com.unitbv.myquiz.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Centralized service for text processing operations.
 * Handles text cleaning, special character removal, enumeration removal, and UTF-8 conversion.
 * <p>
 * This service consolidates text processing logic that was previously scattered
 * across multiple services, improving separation of concerns.
 *
 * @author QuestionService Refactoring
 * @since April 10, 2026
 */
@Service
public class TextProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TextProcessingService.class);
    private static final int MAX_REFERENCE_LENGTH = 2000;
    private static final String[] IMPORT_REFERENCE_EXCLUSIONS = {
            "detalii despre intrebare",
            "detalii din carti",
            "detalii din curs"
    };

    private final EncodingSevice encodingSevice;

    @Autowired
    public TextProcessingService(EncodingSevice encodingSevice) {
        this.encodingSevice = encodingSevice;
    }

    /**
     * Remove special characters from text.
     * Replaces special Unicode characters with ASCII equivalents and normalizes whitespace.
     *
     * @param text Text to clean
     * @return Cleaned text, or empty string if input is null/empty
     */
    public String removeSpecialChars(String text) {
        if (isNullOrEmpty(text)) {
            return "";
        }

        text = text.replace("–", "-");      // en-dash
        text = text.replace("„", "\"");     // double low quote
        text = text.replace("…", "...");    // ellipsis
        text = text.replace("—", "-");      // em-dash
        text = text.replace("\n", " ");     // newline
        text = text.replace("\r", " ");     // carriage return
        text = text.replace("\t", " ");     // tab
        text = text.replace("&", " ");      // ampersand

        // Remove multiple spaces more efficiently
        text = text.replaceAll("\\s+", " ");
        text = text.trim();

        return text;
    }

    /**
     * Remove enumeration markers from text.
     * Removes patterns like "A.", "a.", "1.", "A)", "a)", "1)" etc.
     *
     * @param text Text to clean
     * @return Text without enumeration markers, or empty string if input is null/empty
     */
    public String removeEnumerations(String text) {
        if (isNullOrEmpty(text)) {
            return "";
        }
        return text.replaceAll("[A-Da-d1-4]\\.|[A-Da-d1-4]\\)", "");
    }

    /**
     * Extract simple class name from exception.
     * Removes package name, keeping only the class name.
     *
     * @param e Exception to process
     * @return Simple class name (e.g., "IOException" from "java.io.IOException")
     */
    public String getExceptionClassName(Exception e) {
        if (e == null) {
            return "UnknownException";
        }
        return e.getClass().getName().replaceAll(".*\\.", "");
    }

    /**
     * Comprehensive text cleaning and conversion.
     * Performs UTF-8 conversion, removes enumerations, and removes special characters.
     *
     * @param text Text to clean and convert
     * @return Cleaned and converted text, or empty string if input is null
     */
    public String cleanAndConvert(String text) {
        if (isNullOrEmpty(text)) {
            return "";
        }

        try {
            // Step 1: Convert to UTF-8
            text = encodingSevice.convertToUTF8(text);
            if (isNullOrEmpty(text)) {
                return "";
            }

            // Step 2: Remove enumerations and special characters in sequence
            text = removeEnumerations(text);
            text = removeSpecialChars(text);

            return text != null ? text : "";
        } catch (Exception e) {
            logger.atWarn().setCause(e).log("Error during text cleaning and conversion: {}", e.getMessage());
            return "";
        }
    }

    public boolean containsBlacklistedWords(String text) {
        String[] blacklisted_words = new String[]{"correct", "corect", "raspuns"};

        if (isNullOrEmpty(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return java.util.Arrays.stream(blacklisted_words).anyMatch(lowerText::contains);
    }

    /**
     * Extract and normalize imported references from spreadsheet columns.
     * Returns null if the value is empty or matches known template placeholder text.
     */
    public String prepareImportedReference(String text) {
        String normalized = sanitizeReferenceForSave(text);
        if (isNullOrEmpty(normalized)) {
            return null;
        }

        String lowerReference = normalized.toLowerCase();
        for (String excluded : IMPORT_REFERENCE_EXCLUSIONS) {
            if (lowerReference.contains(excluded)) {
                return null;
            }
        }

        return normalized;
    }

    /**
     * Normalizes user-provided reference text and enforces max length.
     */
    public String sanitizeReferenceForSave(String text) {
        if (text == null) {
            return null;
        }

        String normalized = encodingSevice.convertToUTF8(text);
        if (normalized == null) {
            return null;
        }

        normalized = normalized.replace("\n", " ").replace("\r", " ").replace("\t", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > MAX_REFERENCE_LENGTH) {
            return normalized.substring(0, MAX_REFERENCE_LENGTH);
        }
        return normalized;
    }

    /**
     * Create a text processing pipeline for sequential transformations.
     * Allows chaining multiple text processing operations.
     *
     * @param text Initial text
     * @return Pipeline processor for fluent interface
     */
    public TextProcessingPipeline pipeline(String text) {
        return new TextProcessingPipeline(text, this);
    }

    /**
     * Helper method to check if string is null or empty.
     *
     * @param text String to check
     * @return true if null or empty
     */
    private boolean isNullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * Inner class for fluent text processing pipeline.
     * Allows chaining multiple processing operations.
     */
    public static class TextProcessingPipeline {
        private final TextProcessingService service;
        private String text;

        private TextProcessingPipeline(String text, TextProcessingService service) {
            this.text = text;
            this.service = service;
        }

        /**
         * Add enumeration removal to pipeline.
         *
         * @return this pipeline for chaining
         */
        public TextProcessingPipeline removeEnumerations() {
            this.text = service.removeEnumerations(this.text);
            return this;
        }

        /**
         * Add special character removal to pipeline.
         *
         * @return this pipeline for chaining
         */
        public TextProcessingPipeline removeSpecialChars() {
            this.text = service.removeSpecialChars(this.text);
            return this;
        }

        /**
         * Add UTF-8 conversion to pipeline.
         *
         * @return this pipeline for chaining
         */
        public TextProcessingPipeline convertToUtf8() {
            this.text = service.encodingSevice.convertToUTF8(this.text);
            return this;
        }

        /**
         * Get the processed text result.
         *
         * @return processed text
         */
        public String result() {
            return this.text != null ? this.text : "";
        }
    }
}

