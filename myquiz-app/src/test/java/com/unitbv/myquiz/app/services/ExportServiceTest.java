package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ExportServiceTest {

    Logger logger = LoggerFactory.getLogger(ExportServiceTest.class);

    @Autowired
    ExportService exportService;
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void writeToFileITSecQ1() throws IOException {
        String category = ServiceTestData.SINGLE_FILE_QUESTION_BANK;
        Path outputFile = Files.createTempFile("export-", ".xml");

        TestEntityFactory.QuestionFixture fixture = testEntityFactory.createQuestionFixture(
                ServiceTestData.questionSpecBuilder()
                        .authorName("Export Test Author")
                        .initials("ETA")
                        .course(category)
                        .studyYear(StudyYear.Y2026_2027)
                        .source("export.xlsx")
                        .type(QuestionType.MULTICHOICE)
                        .title("Export Title")
                        .text("Export question text")
                        .build()
        );

        Question question = fixture.question();
        question.setResponse1("A1");
        question.setResponse2("A2");
        question.setResponse3("A3");
        question.setResponse4("A4");
        question.setWeightResponse1(100.0);
        question.setWeightResponse2(-100.0);
        question.setWeightResponse3(-100.0);
        question.setWeightResponse4(-100.0);
        questionRepository.save(question);

        int result;
        try {
            result = exportService.writeToFile(outputFile.toString(), category);
            //log full path
            logger.info("Exported file path: {}", outputFile.toAbsolutePath());
        } finally {
            testEntityFactory.cleanupQuestionFixture(fixture);
        }

        logger.atInfo().addArgument(result).log("Number of exported questions: ");
        assertNotEquals(0, result);
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }

    @Test
    void writeToPdf() throws DocumentException, IOException {
        PdfGenerationService pdfGenerationService = new PdfGenerationService(templateEngine);
        Model model = new ExtendedModelMap();
        model.addAttribute("title", "Question List");
        model.addAttribute("questions", java.util.List.of("Q1", "Q2"));

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        String htmlContent = pdfGenerationService.renderHtmlFromTemplate("question-list", model, request, response);
        assertFalse(htmlContent.isBlank());

        byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml(htmlContent);
        assertTrue(pdfBytes.length > 0);

        Path outputPdf = Files.createTempFile("question-list-", ".pdf");
        pdfGenerationService.writePdfToFile(pdfBytes, outputPdf.toString());
        assertTrue(Files.exists(outputPdf));
        assertTrue(Files.size(outputPdf) > 0);
    }
}
