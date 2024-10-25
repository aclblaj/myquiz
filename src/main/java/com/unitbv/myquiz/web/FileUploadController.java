package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.AuthorServiceImpl;
import com.unitbv.myquiz.services.FileService;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.util.TemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;

import static com.unitbv.myquiz.util.TemplateType.Template2023;
import static com.unitbv.myquiz.util.TemplateType.Template2024;

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
    public String showUploadForm(Model model){
        ArrayList<String> templates = new ArrayList<String>();
        templates.add(Template2023.toString());
        templates.add(Template2024.toString());
        model.addAttribute("templates", templates);
        return "uploadform";
    }

    @PostMapping("/upload")
    public String handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam("course") String course,
            @RequestParam("name") String name,
            @RequestParam("template") String template,
            RedirectAttributes redirectAttributes) {

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

            Quiz quiz = new Quiz();
            quiz.setName(name);
            quiz.setCourse(course);
            TemplateType templateType = TemplateType.valueOf(template);
            quiz.setYear(templateType.getYear());
            questionService.setTemplateType(templateType);
            quiz = questionService.saveQuiz(quiz);
            String result = questionService.readAndParseFirstSheetFromExcelFile(quiz, author, filepath);
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
