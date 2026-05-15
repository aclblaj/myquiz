package com.unitbv.myquiz.app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for validating files and paths.
 * Provides comprehensive file validation for Excel file processing.
 */
public final class FileValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";

    private FileValidator() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that a path exists.
     *
     * @param path The path to validate
     * @return true if path exists, false otherwise
     */
    public static boolean exists(Path path) {
        if (path == null) {
            return false;
        }
        return Files.exists(path);
    }

    /**
     * Validates that a path is readable.
     *
     * @param path The path to validate
     * @return true if path is readable, false otherwise
     */
    public static boolean isReadable(Path path) {
        if (path == null) {
            return false;
        }
        return Files.isReadable(path);
    }

    /**
     * Validates that a path is a directory.
     *
     * @param path The path to validate
     * @return true if path is a directory, false otherwise
     */
    public static boolean isDirectory(Path path) {
        if (path == null) {
            return false;
        }
        return Files.isDirectory(path);
    }

    /**
     * Validates that a path is a regular file.
     *
     * @param path The path to validate
     * @return true if path is a regular file, false otherwise
     */
    public static boolean isRegularFile(Path path) {
        if (path == null) {
            return false;
        }
        return Files.isRegularFile(path);
    }

    /**
     * Checks if a file is an Excel file (.xlsx).
     *
     * @param path The path to check
     * @return true if the file has .xlsx extension, false otherwise
     */
    public static boolean isExcelFile(Path path) {
        if (path == null || !isRegularFile(path)) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(EXCEL_FILE_EXTENSION);
    }

    /**
     * Validates that a path exists, is readable, and is an Excel file.
     *
     * @param path The path to validate
     * @return ValidationResult containing validation status and error message if any
     */
    public static ValidationResult validateExcelFile(Path path) {
        if (path == null) {
            return ValidationResult.error("Path cannot be null");
        }

        if (!exists(path)) {
            return ValidationResult.error("File does not exist: " + path);
        }

        if (!isReadable(path)) {
            return ValidationResult.error("File is not readable: " + path);
        }

        if (!isRegularFile(path)) {
            return ValidationResult.error("Path is not a regular file: " + path);
        }

        if (!isExcelFile(path)) {
            return ValidationResult.error("Invalid file type. Only .xlsx files are supported: " + path);
        }

        return ValidationResult.success();
    }

    /**
     * Lists all files in a directory, returning null if directory cannot be read.
     *
     * @param directory The directory to list
     * @return Array of paths, or null if directory is invalid or cannot be read
     */
    public static Path[] listFiles(Path directory) {
        if (directory == null || !isDirectory(directory)) {
            return null;
        }

        try {
            return Files.list(directory)
                    .sorted()
                    .toArray(Path[]::new);
        } catch (IOException e) {
            logger.atError()
                  .addArgument(directory)
                  .setCause(e)
                  .log("Error listing files in directory: {}");
            return null;
        }
    }

    /**
     * Result of file validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean hasError() {
            return !valid;
        }
    }
}
