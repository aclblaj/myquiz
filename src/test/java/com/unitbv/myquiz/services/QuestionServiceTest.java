package com.unitbv.myquiz.services;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class QuestionServiceTest {

    Logger logger = org.slf4j.LoggerFactory.getLogger(QuestionServiceTest.class);

    @Autowired
    QuestionService questionService;

    @Autowired
    EncodingSevice encodingSevice;

    @Test
    void parseExcelFilesFromFolderITSecQ1() {
        long startTime = (int) System.currentTimeMillis();
        if (encodingSevice.checkServerEncoding()) return;
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-BD\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: {}", result);
        long endTime = (int) System.currentTimeMillis();
        logger.info("Execution time: {} ms", (endTime - startTime));
        assertNotEquals(0, result);
    }

    @Test
    void getServerEncoding() {
        String result = encodingSevice.getServerEncoding();
        logger.info("Server encoding: {}", result);
        assertNotNull(result);
    }
}