package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDataDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.openpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class ExportServiceTest {

    Logger logger = LoggerFactory.getLogger(ExportServiceTest.class);

    @Autowired
    ExportService exportService;
    @Autowired
    private AuthorService authorService;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void writeToFileITSecQ1() {
        //C:\work\_mi\2025-BD\
        String CATEGORY = "25-MITB-MIDB-Q2";
        String OUTPUT_FILE = "C:\\work\\_mi\\2025-MIDB\\" + CATEGORY + ".xml";

        int result = exportService.writeToFile(OUTPUT_FILE, CATEGORY);
        logger.atInfo().addArgument(result)
              .log("Number of exported questions: ");
        assertNotEquals(0, result);
    }

    @Test
    void writeToPdf() throws DocumentException {
        PdfGenerationService pdfGenerationService = new PdfGenerationService(templateEngine);
        Model model = new ExtendedModelMap();

        AuthorDataDto authorDataDto =  authorService.prepareAuthorData("Alecsa Alin Claudiu");

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        String htmlContent = pdfGenerationService.renderHtmlFromTemplate("question-list", model, request, response);

        byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml(htmlContent);
        //write to file
        pdfGenerationService.writePdfToFile(pdfBytes, "C:\\work\\_mi\\2025-MIDB\\testfile.pdf");
    }
}