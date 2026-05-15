package com.unitbv.myquiz.app.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    @Autowired
    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtmlFromTemplate(String templateName, Model model, HttpServletRequest request, HttpServletResponse response) {
        Locale locale = request != null ? request.getLocale() : Locale.getDefault();
        Context context = new Context(locale);
        context.setVariables(model.asMap());
        return templateEngine.process(templateName, context);
    }

    public byte[] generatePdfFromHtml(String htmlContent) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);
        return outputStream.toByteArray();
    }

    public void writePdfToFile(byte[] pdfBytes, String filename) {
        // write pdfBytes to file
        File file = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdfBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
