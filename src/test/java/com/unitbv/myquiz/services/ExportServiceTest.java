package com.unitbv.myquiz.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class ExportServiceTest {

    Logger logger = Logger.getLogger(ExportServiceTest.class.getName());

    @Autowired
    ExportService exportService;

    @Test
    void writeToFileITSecQ1() {
        String CATEGORY = "2024-MIDB-NA-01";
        String OUTPUT_FILE = "C:\\work\\_mi\\2024-ITSec\\" + CATEGORY + ".xml";

        int result = exportService.writeToFile(OUTPUT_FILE, CATEGORY);
        logger.info("Number of exported questions: " + result);
        assertNotEquals(0, result);
    }
}