package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.AuthorServiceImpl;
import com.unitbv.myquiz.services.FileService;
import com.unitbv.myquiz.services.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

    Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    QuestionService questionService;

    AuthorService authorService;

    FileService fileService;

    @Autowired
    public FileUploadController(QuestionService questionService, AuthorServiceImpl authorService, FileService fileService) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.fileService = fileService;
    }

    @GetMapping(value = "/uploadform")
    public String showUploadForm(){
        return "uploadform";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("username") String username, RedirectAttributes redirectAttributes) {

        String message = "";
        String filepath = fileService.uploadFile(file);

        Author author = new Author();
        author.setName(username);
        author.setInitials(authorService.extractInitials(username));
        if (authorService.authorNameExists(author.getName())) {
            logger.atInfo().addArgument(author.getName())
                  .log("Author {} already exists");
            message = "Author " + author.getName() + " already exists";
        } else {
            author = authorService.saveAuthor(author);
            authorService.addAuthorToList(author);

            questionService.setAuthor(author);
            String result = questionService.readAndParseFirstSheetFromExcelFile(filepath);
            logger.atInfo().addArgument(result).log("Result: {}");
            questionService.checkDuplicatesQuestionsForAuthors(authorService.getAuthorsList());
            message = "Successfully uploaded, processed and removed " + filepath + " file";
            fileService.removeFile(filepath);
        }
        redirectAttributes.addFlashAttribute("message", message);
        // Redirect to a success page
        return "redirect:/success";
    }
}
