package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.impl.AuthorServiceImpl;
import com.unitbv.myquiz.services.FileService;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.services.QuestionValidationService;
import com.unitbv.myquiz.services.CourseService;
import com.unitbv.myquizapi.dto.CourseDto;
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
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.unitbv.myquiz.util.TemplateType.Template2023;
import static com.unitbv.myquiz.util.TemplateType.Template2024;

@Controller
public class FileUploadController {

    Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    QuestionService questionService;

    AuthorService authorService;

    FileService fileService;

    private final QuestionValidationService questionValidationService;

    CourseService courseService;

    @Autowired
    public FileUploadController(
            QuestionService questionService,
            AuthorService authorService,
            QuestionValidationService questionValidationService,
            FileService fileService,
            CourseService courseService) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.questionValidationService = questionValidationService;
        this.fileService = fileService;
        this.courseService = courseService;
    }

    @GetMapping(value = "/uploadform")
    public String showUploadForm(Model model){
        ArrayList<String> templates = new ArrayList<String>();
        templates.add(Template2023.toString());
        templates.add(Template2024.toString());
        model.addAttribute("templates", templates);
        List<CourseDto> courses = courseService.getAllCourses();
        model.addAttribute("courses", courses);
        return "uploadform";
    }

    @PostMapping("/upload")
    public String handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam("courseId") Long courseId,
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

            if (courseId == null || courseId == 0) {
                message = "No course selected!";
                redirectAttributes.addFlashAttribute("message", message);
                return "redirect:/success";
            }
            String course = courseService.findById(courseId).getCourse();

            Quiz quiz = new Quiz();
            quiz.setName(name);
            quiz.setCourse(course);
            TemplateType templateType = TemplateType.valueOf(template);
            quiz.setYear(templateType.getYear());
            questionService.setTemplateType(templateType);
            quiz = questionService.saveQuiz(quiz);
            String result = questionService.readAndParseFirstSheetFromExcelFile(quiz, author, filepath);
            logger.atInfo().addArgument(result).log("Result: {}");
            questionValidationService.checkDuplicatesQuestionsForAuthors(authorService.getAuthorsList(), quiz.getCourse());
            message = "Successfully uploaded, processed and removed " + filepath + " file";
            fileService.removeFile(filepath);
        }
        redirectAttributes.addFlashAttribute("message", message);
        // Redirect to a success page
        return "redirect:/success";
    }

    @PostMapping("/upload-archive")
    public String handleArchiveUpload(@RequestParam("archive") MultipartFile archive,
                                      @RequestParam("courseId") Long courseId,
                                      @RequestParam("quiz") String quizName,
                                      @RequestParam("year") Long year,
                                      RedirectAttributes redirectAttributes) throws IOException {
        Path tempDir = null;
        try {
            // Create temp directory
            tempDir = Files.createTempDirectory("uploaded-archive-");
            // Check write permission
            if (!Files.isWritable(tempDir)) {
                throw new IOException("Temp directory is not writable: " + tempDir.toString());
            }

            //get full path to the zip file
            String archivePath = fileService.uploadFile(archive);
            Path archiveFilePath = Path.of(archivePath);
            logger.atInfo().addArgument(archivePath).log("Archive uploaded to: {}");

            fileService.unzipAndRenameExcelFiles(archiveFilePath, tempDir);

            // Get course code from courseId
            CourseDto courseDto = courseService.getAllCourses().stream()
                .filter(c -> c.getId().equals(courseId))
                .findFirst().orElse(null);
            if (courseDto == null) {
                FileSystemUtils.deleteRecursively(tempDir);
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }
            // Create Quiz
            Quiz quiz = new Quiz();
            quiz.setName(quizName);
            quiz.setCourse(courseDto.getCourse());
            quiz.setYear(year);
            quiz = questionService.saveQuiz(quiz);
            // Import questions from unpacked folder
            int result;
            try {
                result = questionService.parseExcelFilesFromFlatFolder(quiz, tempDir.toFile());
            } catch (Exception ex) {
                throw new IOException("Failed to parse Excel files: " + ex.getMessage(), ex);
            }
            // Cleanup
            FileSystemUtils.deleteRecursively(tempDir);
            redirectAttributes.addFlashAttribute("message", "Imported " + result + " files successfully.");
            fileService.removeFile(archivePath);
            logger.atInfo().addArgument(result).log("Number of imported files: {}");
        } catch (Exception e) {
            if (tempDir != null) FileSystemUtils.deleteRecursively(tempDir);
            logger.error("Archive upload failed", e);
            redirectAttributes.addFlashAttribute("message", "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return "redirect:/success";

    }
}
