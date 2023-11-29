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

    @Test
    void parseExcelFilesFromFolder() {
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-BD\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }

    @Test
    void parseExcelFilesFromFolderVDBQ1() {
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-VDB\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }


    @Test
    void parseExcelFilesFromFolderITSecQ1() {
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-ITSec\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }

    @Test
    void parseExcelFilesFromFolderBDQ1T() {
        final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-BD2\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }
}