package com.unitbv.myquiz.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuestionServiceTest {

    Logger logger = Logger.getLogger(QuestionServiceTest.class.getName());

    @Autowired
    QuestionService questionService;

    @Autowired
    EncodingSevice encodingSevice;

    @Test
    void parseExcelFilesFromFolderITSecQ1() {
        if (encodingSevice.checkServerEncoding()) return;
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-ITSec\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }

    @Test
    void getServerEncoding() {
        String result = encodingSevice.getServerEncoding();
        logger.info("Server encoding: " + result);
        assertNotNull(result);
    }
}