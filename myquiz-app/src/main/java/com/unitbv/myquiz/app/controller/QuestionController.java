package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterDto;
import com.unitbv.myquiz.api.dto.QuestionFilterInputDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuizInfo;
import com.unitbv.myquiz.api.interfaces.QuestionApi;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.QuestionService;
import com.unitbv.myquiz.app.services.QuizService;
import com.unitbv.myquiz.app.services.QuizAuthorService;
import com.unitbv.myquiz.app.services.QuestionCorrectionService;
import com.unitbv.myquiz.app.web.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/questions")
@Tag(name = "Questions", description = "Operations related to quiz questions")
public class QuestionController implements QuestionApi {

    // Use SLF4J's log.info for consistency and compatibility
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    // Use constructor injection for all dependencies
    private final QuestionService questionService;
    private final AuthorService authorService;
    private final QuizService quizService;
    private final CourseService courseService;
    private final QuizAuthorService quizAuthorService;
    private final QuestionCorrectionService questionCorrectionService;

    private static final String LOG_QUESTION_NOT_FOUND = "Question not found with id: {}";

    public QuestionController(QuestionService questionService, AuthorService authorService,
                              QuizService quizService, CourseService courseService,
                              QuizAuthorService quizAuthorService,
                              QuestionCorrectionService questionCorrectionService) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.quizService = quizService;
        this.courseService = courseService;
        this.quizAuthorService = quizAuthorService;
        this.questionCorrectionService = questionCorrectionService;
    }

    @Override
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        log.info("Getting all questions");
        try {
            List<QuestionDto> questions = questionService.getAllQuestions();
            return ResponseEntity.ok(questions != null ? questions : new ArrayList<>());
        } catch (Exception e) {
            log.error("Error getting all questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<QuestionDto> getQuestionById(@PathVariable Long id) {
        log.info("Getting question by id: {}", id);
        try {
            QuestionDto question = questionService.getQuestionById(id);
            return question != null ? ResponseEntity.ok(question) : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.warn(LOG_QUESTION_NOT_FOUND, id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting question by id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping({"", "/"})
    @Override
    public ResponseEntity<QuestionDto> createQuestion(@RequestBody QuestionDto questionDto) {
        log.info("Creating new question");
        try {
            if (questionDto == null) {
                log.warn("Received null question data");
                return ResponseEntity.badRequest().build();
            }
            QuestionDto createdQuestion = questionService.createQuestion(questionDto);
            if (createdQuestion == null) {
                log.error("Service returned null after creating question");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
        } catch (Exception e) {
            log.error("Error creating question", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<QuestionDto> updateQuestion(@PathVariable Long id, @RequestBody QuestionDto questionDto) {
        log.info("Updating question with id: {}", id);
        try {
            questionDto.setId(id);
            QuestionDto updatedQuestion = questionService.updateQuestion(questionDto);
            return updatedQuestion != null ? ResponseEntity.ok(updatedQuestion) : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.warn(LOG_QUESTION_NOT_FOUND, id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating question with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        log.info("Deleting question with id: {}", id);
        try {
            boolean deleted = questionService.deleteQuestion(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.warn(LOG_QUESTION_NOT_FOUND, id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting question with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/quiz/{quizId}")
    @Override
    public ResponseEntity<QuestionFilterDto> getQuestionsByQuizId(@PathVariable Long quizId) {
        log.info("Getting questions by quiz id: {}", quizId);
        try {
            QuizDto quiz = quizService.getQuizById(quizId);
            if (quiz == null) {
                log.warn("Quiz not found with id: {}", quizId);
                return ResponseEntity.notFound().build();
            }
            QuestionFilterInputDto filterInput = new QuestionFilterInputDto();
            filterInput.setQuiz(quizId);
            filterInput.setCourse(quiz.getCourse());
            List<QuestionDto> questions = questionService.getQuestionsByQuizId(quizId);
            if (questions == null) {
                questions = new ArrayList<>();
            }
            QuestionFilterDto filteredQuestionsDto = new QuestionFilterDto();
            filteredQuestionsDto.setQuestions(questions);
            filteredQuestionsDto.setAuthors(quiz.getAuthorInfos());
            filteredQuestionsDto.setSelectedQuiz(quiz);
            filteredQuestionsDto.setCourse(quiz.getCourse() != null ? quiz.getCourse() : "");
            return ResponseEntity.ok(filteredQuestionsDto);
        } catch (Exception e) {
            log.error("Error getting questions by quiz id: {}", quizId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/filter")
    @Override
    public ResponseEntity<QuestionFilterDto> listQuestionsFiltered(@RequestBody QuestionFilterInputDto filterInput) {
        log.info("Filtering questions with input: {}", filterInput);

        AuthorInfo authorInfo = resolveAuthorInfo(filterInput.getAuthor());
        String selectedCourse = filterInput.getCourse();

        QuestionFilterDto filteredQuestions = questionService.getQuestionsFiltered(
                selectedCourse, authorInfo.getId(),
                filterInput.getPage(), filterInput.getPageSize(),
                filterInput.getQuiz(),
                filterInput.getQuestionType()
        );

        enrichFilteredQuestionsDto(filteredQuestions, selectedCourse);
        filteredQuestions.setAuthorName(authorInfo.getName());
        filteredQuestions.setSelectedCourse(selectedCourse);

        return ResponseEntity.ok(filteredQuestions);
    }

    /**
     * Resolves author information from input (can be ID or name)
     */
    private AuthorInfo resolveAuthorInfo(String authorInput) {
        Long authorId = null;
        String authorName = "";

        if (authorInput != null) {
            try {
                // Try to parse as Long (author ID)
                authorId = Long.valueOf(authorInput);
                AuthorDto author = authorService.getAuthorById(authorId);
                if (author != null) {
                    authorName = author.getName();
                }
            } catch (NumberFormatException ex) {
                // Not a number, treat as author name
                authorName = authorInput;
                AuthorDto author = authorService.getAuthorByName(authorName);
                if (author != null) {
                    authorId = author.getId();
                }
            }
        }

        return new AuthorInfo(authorId, authorName);
    }

    /**
     * Enriches the filtered questions DTO with courses, authors, and quizzes
     */
    private void enrichFilteredQuestionsDto(QuestionFilterDto filteredQuestions, String selectedCourse) {
        // Add all course names
        List<String> courses = courseService.getAllCourseNames();
        filteredQuestions.setCourses(courses.toArray(new String[0]));
        filteredQuestions.setCourse(selectedCourse);

        if (selectedCourse != null && !selectedCourse.isEmpty()) {
            // Course-specific filtering
            populateCourseSpecificData(filteredQuestions, selectedCourse);
        } else {
            // No course selected - populate all data
            populateAllData(filteredQuestions);
        }
    }

    /**
     * Populates course-specific authors and quizzes
     */
    private void populateCourseSpecificData(QuestionFilterDto filteredQuestions, String selectedCourse) {
        // Use lightweight method that returns only basic author info (id, name, initials)
        // without expensive question count computations - significant performance improvement
        List<AuthorInfo> authors = authorService.getAuthorsByCourse(selectedCourse);
        filteredQuestions.setAuthors(authors != null ? authors : new ArrayList<>());

        List<QuizInfo> courseQuizzes = quizService.getQuizInfoByCourse(selectedCourse);
        filteredQuestions.setQuizzes(courseQuizzes != null ? courseQuizzes : new ArrayList<>());
    }

    /**
     * Populates all authors and quizzes when no course is selected
     */
    private void populateAllData(QuestionFilterDto filteredQuestions) {
        // Use lightweight method that returns only basic author info (id, name, initials)
        // without expensive question count computations
        List<AuthorInfo> allAuthors = authorService.getAllAuthorsBasic();
        filteredQuestions.setAuthors(allAuthors != null ? allAuthors : new ArrayList<>());

        List<QuizInfo> allQuizzes = quizService.getAllQuizInfo();
        filteredQuestions.setQuizzes(allQuizzes != null ? allQuizzes : new ArrayList<>());
    }

    @GetMapping("/author/{authorId}/quiz/{quizId}")
    @Operation(summary = "Get questions by author and quiz",
               description = "Retrieve all questions for a specific quiz created by a specific author")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
            @ApiResponse(responseCode = "404", description = "Quiz author combination not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QuestionFilterDto> getQuestionsByAuthorAndQuiz(
            @Parameter(description = "Author ID", required = true) @PathVariable Long authorId,
            @Parameter(description = "Quiz ID", required = true) @PathVariable Long quizId) {
        log.info("Getting questions by author {} and quiz {}", authorId, quizId);
        try {
            var qaOpt = quizAuthorService.getQuizAuthorByQuizIdAndAuthorId(quizId, authorId);
            if (qaOpt.isEmpty()) {
                log.warn("No QuizAuthor found for author {} and quiz {}", authorId, quizId);
                return ResponseEntity.notFound().build();
            }
            var quizAuthor = qaOpt.get();
            QuizDto quiz = quizService.getQuizById(quizId);
            if (quiz == null) {
                return ResponseEntity.notFound().build();
            }
            List<QuestionDto> questions = questionService.getQuizzQuestionsForAuthor(quizAuthor.getId()).stream()
                    .map(q -> questionService.getQuestionById(q.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            QuestionFilterDto dto = new QuestionFilterDto();
            dto.setQuestions(questions);
            dto.setSelectedQuiz(quiz);
            dto.setCourse(quiz.getCourse() != null ? quiz.getCourse() : "");
            List<AuthorInfo> authors = authorService.getAuthorsByCourse(dto.getCourse());
            dto.setAuthors(authors != null ? authors : new ArrayList<>());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error getting questions by author {} and quiz {}", authorId, quizId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/correction/grammar")
    @Operation(summary = "Correct grammar in question", description = "Use AI to correct grammar and spelling errors")
    public ResponseEntity<QuestionCorrectionDto> correctGrammar(@PathVariable("id") Long id,
                                                                @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("[APP] Processing grammar correction for question {}", id);
            QuestionCorrectionDto result = questionCorrectionService.correctGrammar(correctionDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error correcting grammar", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/improve")
    @Operation(summary = "Improve question", description = "Use AI to improve question clarity and precision")
    public ResponseEntity<QuestionCorrectionDto> improveQuestion(@PathVariable("id") Long id,
                                                                 @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("[APP] Processing question improvement for question {}", id);
            QuestionCorrectionDto result = questionCorrectionService.improveQuestion(correctionDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error improving question", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/alternatives")
    @Operation(summary = "Generate alternative answers", description = "Use AI to generate plausible but incorrect alternatives")
    public ResponseEntity<Map<String, String>> generateAlternatives(@PathVariable("id") Long id,
                                                                    @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("[APP] Processing generate alternatives for question {}", id);
            String alternatives = questionCorrectionService.generateAlternatives(correctionDto);
            Map<String, String> response = new HashMap<>();
            response.put("alternatives", alternatives);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating alternatives", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/explanation")
    @Operation(summary = "Explain correct answer", description = "Use AI to explain why an answer is correct and others are wrong")
    public ResponseEntity<Map<String, String>> explainAnswer(@PathVariable("id") Long id,
                                                             @RequestBody QuestionCorrectionDto correctionDto) {
        log.info("[APP] Processing explain answer for question {}", id);
        try {
            String explanation = questionCorrectionService.explainAnswer(correctionDto);
            Map<String, String> response = new HashMap<>();
            response.put("explanation", explanation);
            response.put("status", "success");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Explain answer interrupted for question {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error explaining answer for question {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
