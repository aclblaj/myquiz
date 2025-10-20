package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.CourseService;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.services.QuizAuthorService;
import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquizapi.dto.AuthorDto;
import com.unitbv.myquizapi.dto.AuthorErrorDto;
import com.unitbv.myquizapi.dto.QuestionDto;
import com.unitbv.myquizapi.dto.QuizDto;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value={"/questions", "/question-list"})
public class QuestionWebController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionWebController.class);

    private final QuestionService questionService;

    private final AuthorErrorService authorErrorService;

    private final AuthorService authorService;

    private final QuizAuthorService quizAuthorService;

    private final QuizService quizService;

    private final CourseService courseService;

    @Autowired
    public QuestionWebController(QuestionService questionService,
                                 AuthorErrorService authorErrorService,
                                 AuthorService authorService,
                                 QuizAuthorService quizAuthorService,
                                 QuizService quizService,
                                 CourseService courseService) {
        this.questionService = questionService;
        this.authorErrorService = authorErrorService;
        this.authorService = authorService;
        this.quizAuthorService = quizAuthorService;
        this.quizService = quizService;
        this.courseService = courseService;
    }

    public static int compare(QuestionDto q1, QuestionDto q2) {
        if (q1 == null || q2 == null) {
            return 0;
        }
        if (q1.getCourse() == null || q2.getCourse() == null) {
            return 0;
        }
        if (q1.getRow() == null || q2.getRow() == null) {
            return 0;
        }
        int courseCompare = q1.getCourse().compareTo(q2.getCourse());
        if (courseCompare != 0) {
            return courseCompare;
        }
        return q1.getRow() - q2.getRow();
    }

    @GetMapping("/list")
    public String listQuestions(Model model, HttpSession session) {
        List<QuestionDto> questionDtos = new ArrayList<>();
        List<Author> authors = authorService.getAllAuthors();
        Long selectedAuthorId = null;
        String authorName = null;
        if (authorName == null && !authors.isEmpty()) {
            authorName = authors.get(0).getName();
            selectedAuthorId = authors.get(0).getId();
        }

        Object selectedAuthorObj = session.getAttribute("selectedAuthor");
        if (selectedAuthorObj instanceof Long) {
            selectedAuthorId = (Long) selectedAuthorObj;
            Author author = authorService.getAuthorById(selectedAuthorId);
            if (author != null) {
                authorName = author.getName();
            }
        }

        questionService.getQuestionsForAuthorName(authorName).forEach(
                question -> {
                    QuestionDto dto = new QuestionDto();
                    dto.setId(question.getId());
                    dto.setTitle(question.getTitle());
                    dto.setText(question.getText());
                    dto.setChapter(question.getChapter());
                    questionDtos.add(dto);
                });
        model.addAttribute("questions", questionDtos);
        model.addAttribute("authors", authors);
        model.addAttribute("selectedAuthorId", selectedAuthorId);
        model.addAttribute("selectedAuthor", authorName);

        // Fetch a quiz for the selected author (if available)
        QuizDto quiz = null;
        if (selectedAuthorId != null) {
            List<QuizDto> quizzes = quizService.getAllQuizzes();
            for (QuizDto q : quizzes) {
                if (q.getQuizAuthorId() != null && q.getQuizAuthorId().equals(selectedAuthorId)) {
                    quiz = q;
                    break;
                }
            }
        }
        model.addAttribute("quiz", quiz);

        return "question-list";
    }

    @PostMapping("/dodeleteops")
    public String deleteOps(
            @RequestParam("course") String selectedCourse,
            RedirectAttributes redirectAttributes
    ) {

        if (selectedCourse == null) {
            redirectAttributes.addFlashAttribute("message", "No course selected for deletion");
            return "redirect:/error";
        }
        courseService.deleteCourse(selectedCourse);
        logger.atInfo()
              .addArgument(selectedCourse)
              .log("The selected course '{}' was deleted");

        redirectAttributes.addFlashAttribute("message", "Successfully deleted tables content for course " + selectedCourse);
        return "redirect:/success";
    }

    @GetMapping("/deleteops")
    public String deleteSelectedQuiz(
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<String> courses = authorService.getCourseNames();
        String selectedCourse = null;
        if ( selectedCourse == null) {
            selectedCourse = courses.size() > 0 ? courses.get(0) : "";
        }

        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourse", selectedCourse);
        String route = "questions/dodeleteops";
        model.addAttribute("route", route);

        logger.atInfo()
                .addArgument(selectedCourse)
                .addArgument(route)
                .log("Deleting the selected course '{}' and go to route '{}'");

        redirectAttributes.addFlashAttribute("selectedCourse", selectedCourse);
        return "do-delete-ops";
    }

    @GetMapping("/deleteall")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        logger.atInfo().log("Deleting all authors/quizzes/questions");
        quizAuthorService.deleteAll();
        quizService.deleteAll();
        authorErrorService.deleteAll();
        questionService.deleteAllQuestions();
        authorService.deleteAll();
        redirectAttributes.addFlashAttribute("message", "Successfully deleted tables content.");
        return "redirect:/success";
    }
    
    @GetMapping("/{course}")
    public String listQuestionsByCourse(@PathVariable String course, Model model) {
        logger.atInfo()
                .addArgument(course)
                .log("Listing questions for course: {}");
        
        // Get quizzes for the course
        List<QuizDto> quizzes = quizService.getQuizzesByCourse(course);
        
        // For each quiz, get its questions and errors
        for (QuizDto quiz : quizzes) {
            // Fetch detailed quiz info which includes questions
            QuizDto detailedQuiz = quizService.getQuizById(quiz.getId());
            
            if (detailedQuiz != null) {
                quiz.setQuestionsMC(detailedQuiz.getQuestionsMC());
                quiz.setQuestionsTF(detailedQuiz.getQuestionsTF());
            } else {
                quiz.setQuestionsMC(new ArrayList<>());
                quiz.setQuestionsTF(new ArrayList<>());
            }
            
            // Get author errors for this quiz
            List<AuthorErrorDto> errors = authorErrorService.getErrorsByQuizId(quiz.getId());
            quiz.setAuthorErrorDtos(errors);
        }
        
        // Get unique authors for the course
        List<Author> authors = authorService.getAuthorsByCourse(course);


        model.addAttribute("authors", authors);

        // Create a simple author object with the course as the name
        AuthorDto author = new AuthorDto();
        author.setName("Course: " + course);
        
        model.addAttribute("author", author);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("course", course);
        return "question-list";
    }
    
    @GetMapping("/filtered")
    public String listQuestionsFiltered(@RequestParam(value = "author", required = false) Long authorId,
                                        @RequestParam(value = "course", required = false) String course,
                                        Model model, HttpSession session) {
        List<QuestionDto> questionDtos = new ArrayList<>();
        List<Author> authors = authorService.getAllAuthors();
        Long selectedAuthorId = authorId;
        String authorName = null;
        if (selectedAuthorId != null) {
            Author author = authorService.getAuthorById(selectedAuthorId);
            if (author != null) {
                authorName = author.getName();
            }
            // Update session variable for selectedAuthor
            session.setAttribute("selectedAuthor", selectedAuthorId);
        } else {
            // If no author specified, use all authors and clear session
            session.removeAttribute("selectedAuthor");
        }
        // If no author specified, use all authors
        if (authorName == null) {
            authorName = null;
        }
        // If no course specified, use all courses
        String selectedCourse = course;
        if (selectedCourse != null) {
            session.setAttribute("selectedCourse", selectedCourse);
        } else {
            session.removeAttribute("selectedCourse");
        }
        // Get questions filtered by author and course
        List<QuestionDto> filteredQuestions = questionService.getQuestionsFiltered(selectedCourse, selectedAuthorId);
        model.addAttribute("questions", filteredQuestions);
        model.addAttribute("authors", authors);
        model.addAttribute("selectedAuthorId", selectedAuthorId);
        model.addAttribute("selectedAuthor", authorName);
        model.addAttribute("selectedCourse", selectedCourse);
        return "question-list";
    }

    @GetMapping("")
    public String listQuestionsByAuthor(@RequestParam(value = "author", required = false) Long authorId, Model model, HttpSession session) {
        logger.atInfo().log("Listing questions for authorId: {}", authorId);
        List<QuestionDto> questionDtos = new ArrayList<>();
        List<Author> authors = authorService.getAllAuthors();
        String authorName = null;
        if (authorId != null) {
            Author author = authorService.getAuthorById(authorId);
            if (author != null) {
                authorName = author.getName();
            }
        }
        if (authorName == null && !authors.isEmpty()) {
            authorName = authors.get(0).getName();
            authorId = authors.get(0).getId();
        }
        logger.atInfo().log("Selected authorName: {}", authorName);

        if (authorId != null) {
            session.setAttribute("selectedAuthor", authorName);
            session.setAttribute("selectedAuthorId", authorId);
            model.addAttribute("selectedAuthorId", authorId);
            model.addAttribute("selectedAuthor", authorName);
        }

        authorService.prepareAuthorModelData(model, authorName);
        return "question-list";
    }
}
