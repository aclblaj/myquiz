package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorServiceImpl;
import com.unitbv.myquiz.services.FileService;
import com.unitbv.myquiz.services.QuestionServiceImpl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

    Logger logger = org.slf4j.LoggerFactory.getLogger(FileUploadController.class);

    QuestionServiceImpl questionServiceImpl;

    AuthorServiceImpl authorServiceImpl;

    FileService fileService;

    @Autowired
    public FileUploadController(QuestionServiceImpl questionServiceImpl, AuthorServiceImpl authorServiceImpl, FileService fileService) {
        this.questionServiceImpl = questionServiceImpl;
        this.authorServiceImpl = authorServiceImpl;
        this.fileService = fileService;
    }

    @GetMapping(value = "/uploadform")
    public String showUploadForm(){
        return "uploadform";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("username") String username, RedirectAttributes redirectAttributes) {

        String filepath = fileService.uploadFile(file);

        Author author = new Author();
        author.setName(username);
        author.setInitials(authorServiceImpl.extractInitials(username));
        author = authorServiceImpl.saveAuthor(author);

        questionServiceImpl.setAuthor(author);
        String result = questionServiceImpl.readAndParseFirstSheetFromExcelFile(filepath);
        logger.info("Result: {}", result);

        fileService.removeFile(filepath);

        redirectAttributes.addFlashAttribute("message", "Successfully uploaded, processed and removed: " + filepath);
        // Redirect to a success page
        return "redirect:/success";
    }
}
