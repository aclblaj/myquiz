package com.unitbv.myquiz.services;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class ExportServiceTest {

    Logger logger = LoggerFactory.getLogger(ExportServiceTest.class);

    @Autowired
    ExportService exportService;

    @Test
    void writeToFileITSecQ1() {
        String CATEGORY = "2024-01";
        String OUTPUT_FILE = "c:\\work\\" + CATEGORY + "0099.xml";

        int result = exportService.writeToFile(OUTPUT_FILE, CATEGORY);
        logger.atInfo().addArgument(result)
              .log("Number of exported questions: ");
        assertNotEquals(0, result);
    }
}