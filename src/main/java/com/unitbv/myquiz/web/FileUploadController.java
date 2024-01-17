package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.QuestionService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class FileUploadController {

    @Value("${upload.dir}")
    private String uploadDir;
    Logger logger = org.slf4j.LoggerFactory.getLogger(FileUploadController.class);

    QuestionService questionService;

    AuthorService authorService;

    @Autowired
    public FileUploadController(QuestionService questionService, AuthorService authorService) {
        this.questionService = questionService;
        this.authorService = authorService;
    }

    @GetMapping(value = "/uploadform")
    public String showUploadForm(){
        return "uploadform";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("username") String username, RedirectAttributes redirectAttributes) {
        // Here, you can save the uploaded file to a directory
        // For example:
        String filepath = "not found";
        try {
            filepath = uploadDir + File.separator + file.getOriginalFilename();
            Files.copy(file.getInputStream(), Paths.get(filepath));
            logger.info("File uploaded to: {}", filepath);
        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage());

        }

        Author author = new Author();
        author.setName(username);
        author.setInitials(authorService.extractInitials(username));
        author = authorService.saveAuthor(author);

        questionService.setAuthor(author);
        String result = questionService.readAndParseFirstSheetFromExcelFile(filepath);
        logger.info("Result: {}", result);

        redirectAttributes.addFlashAttribute("message", "Successfully uploaded: " + filepath);
        // Redirect to a success page
        return "redirect:/success";
    }
}
