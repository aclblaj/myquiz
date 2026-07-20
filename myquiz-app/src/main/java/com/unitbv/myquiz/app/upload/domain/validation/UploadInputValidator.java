package com.unitbv.myquiz.app.upload.domain.validation;

import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Component
public class UploadInputValidator {
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";

    public void validateExcelUploadInputs(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(EXCEL_FILE_EXTENSION)) {
            throw new IllegalArgumentException("Only .xlsx files are supported for single Excel upload");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (questionBankName == null || questionBankName.isBlank()) {
            throw new IllegalArgumentException("QuestionBank name is required");
        }
        if (templateType == null) {
            throw new IllegalArgumentException("Template type is required");
        }
    }

    public void validateXmlUploadInputs(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear) {
        if (xml == null || xml.isEmpty()) {
            throw new IllegalArgumentException("XML file is required");
        }
        String originalFilename = xml.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new IllegalArgumentException("Only .xml files are supported for XML upload");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (questionBankName == null || questionBankName.isBlank()) {
            throw new IllegalArgumentException("QuestionBank name is required");
        }
        if (studyYear == null) {
            throw new IllegalArgumentException("Study year is required");
        }
    }
}

