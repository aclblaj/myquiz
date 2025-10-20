package com.unitbv.myquiz.services;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.WebContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
@Service
public class PdfGenerationService {

    private TemplateEngine templateEngine;

    @Autowired
    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtmlFromTemplate(String templateName, Model model, HttpServletRequest request, HttpServletResponse response) {
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        WebContext context = null; // new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap(), webApplicationContext);
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
