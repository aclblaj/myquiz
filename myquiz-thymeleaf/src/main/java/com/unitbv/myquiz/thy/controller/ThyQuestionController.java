package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import com.unitbv.myquiz.api.dto.QuestionFilterInputDto;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.thy.service.QuestionCorrectionService;
import jakarta.validation.Valid;
import org.springframework.web.client.RestTemplate;

/**
 * Thymeleaf controller for Question management operations.
 * Handles question listing, filtering, creation, editing, and deletion.
 * Supports both multiple choice and true/false question types.
 * Provides server-side rendering for question-related pages.
 */
@Controller
@RequestMapping({"/questions"})
public class ThyQuestionController {
    private static final Logger log = LoggerFactory.getLogger(ThyQuestionController.class);

    // Constants for repeated strings
    private static final String QUESTION_NOT_FOUND = "Question not found";
    private static final String LOG_QUESTION_NOT_FOUND = "Question {} not found";
    private static final String ERROR_LOADING_QUESTION = "Error loading question";
    private static final String REDIRECT_QUESTIONS_PREFIX = "redirect:/questions/";

    private final RestTemplate restTemplate;
    private final SessionService sessionService;
    private final QuestionCorrectionService correctionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyQuestionController(SessionService sessionService, RestTemplate restTemplate,
                                 QuestionCorrectionService correctionService) {
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
        this.correctionService = correctionService;
    }

    private static String getQuestionView(String type) {
        log.info("Determining view for question type: {}", type);
        String viewName;
        if (type != null) {
            if (type.equalsIgnoreCase(QuestionType.MULTICHOICE.name())) {
                viewName = ControllerSettings.QUESTION_EDITOR_MULTICHOICE;
            } else if (type.equalsIgnoreCase(QuestionType.TRUEFALSE.name())) {
                viewName = ControllerSettings.QUESTION_EDITOR_TRUEFALSE;
            } else {
                viewName = ControllerSettings.VIEW_QUESTION_LIST;
            }
        } else {
            viewName = ControllerSettings.VIEW_QUESTION_LIST;
        }
        return viewName;
    }

    @GetMapping({"/", ""})
    public String listAllQuestions(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                   @RequestParam(value = "course", required = false) String course,
                                   @RequestParam(value = "authorId", required = false) Long authorId,
                                   @RequestParam(value = "type", required = false) String type,
                                   @RequestParam(value = "quizId", required = false) Long quizId,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                   Model model) {
        log.info("Listing all questions");
        return renderQuestionList(page, course, authorId != null ? String.valueOf(authorId) : null, type, quizId, pageSize, model);
    }

    @PostMapping("/filter")
    public String filterQuestions(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                  @RequestParam(value = "course", required = false) String course,
                                  @RequestParam(value = "authorId", required = false) Long authorId,
                                  @RequestParam(value = "type", required = false) String type,
                                  @RequestParam(value = "quizId", required = false) Long quizId,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                  Model model) {
        log.info("Filtering questions");
        return renderQuestionList(page, course, authorId != null ? String.valueOf(authorId) : null, type, quizId, pageSize, model);
    }

    @GetMapping("/author/{authorId}")
    public String listQuestionsByAuthor(@PathVariable Long authorId,
                                        @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                        @RequestParam(value = "course", required = false) String course,
                                        @RequestParam(value = "type", required = false) String type,
                                        @RequestParam(value = "quizId", required = false) Long quizId,
                                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                        Model model) {
        log.info("Listing questions by author ID: {}", authorId);
        return renderQuestionList(page, course, String.valueOf(authorId), type, quizId, pageSize, model);
    }

    @GetMapping({
        "/filter/{course}/{authorId}/{page}/{pageSize}",
        "/filter/{course}/{quizId}/{authorId}/{type}/{page}/{pageSize}"
    })
    public String filterQuestionsWithPathParams(
            @PathVariable("course") String course,
            @PathVariable(value = "authorId", required = false) Long authorId,
            @PathVariable(value = "quizId", required = false) Long quizId,
            @PathVariable(value = "type", required = false) String type,
            @PathVariable("page") Integer page,
            @PathVariable("pageSize") Integer pageSize,
            Model model) {

        log.info("Filtering questions - course: {}, authorId: {}, quizId: {}, type: {}, page: {}, pageSize: {}",
                course, authorId, quizId, type, page, pageSize);

        // Determine author parameter (name takes precedence over ID)
        String author = authorId != null ? String.valueOf(authorId) : null;
        return renderQuestionList(page, course, author, type, quizId, pageSize, model);
    }

    private String renderQuestionList(Integer page, String course, String author, String type, Long quizId, Integer pageSize, Model model) {
        log.info("Listing all questions with filters - page: {}, course: {}, author: {}, type: {}, quizId: {}, pageSize: {}",
                         page, course, author, type, quizId, pageSize);

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUESTIONS, new QuestionDto[0]);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, new AuthorDto[0]);
            model.addAttribute(ControllerSettings.ATTR_QUIZZES, new QuizDto[0]);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, null);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Please log in to view questions.");
            return ControllerSettings.VIEW_QUESTION_LIST;
        }

        // Normalize empty strings to null
        if (course != null && course.trim().isEmpty()) {
            course = null;
        }
        if (type != null && type.trim().isEmpty()) {
            type = null;
        }
        if (author != null && author.trim().isEmpty()) {
            author = null;
        }

        if (pageSize == null || pageSize < 1) {
            pageSize = ControllerSettings.PAGE_SIZE;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        try {
            String filterUrl = apiBaseUrl + ControllerSettings.API_QUESTIONS_FILTER;
            QuestionType questionType = getQuestionTypeFromString(type);
            QuestionFilterInputDto filterInputDto = QuestionFilterInputDto.builder()
                .course(course)
                .author(author)  // Pass author directly (can be ID or name, backend handles it)
                .questionType(questionType)
                .quiz(quizId)
                .page(page)
                .pageSize(pageSize)
                .build();
            log.info("Filtering questions with filter: {}", filterInputDto);

            HttpEntity<QuestionFilterInputDto> requestEntity = sessionService.createAuthorizedRequest(filterInputDto);

            ResponseEntity<QuestionFilterDto> response = restTemplate.exchange(
                filterUrl,
                HttpMethod.POST,
                requestEntity,
                QuestionFilterDto.class
            );
            QuestionFilterDto filterDto = response.getBody();
            if (filterDto == null) {
                log.error("API returned null for QuestionFilterDto");
                populateQuestionListModelFallback(model, page, pageSize, course, type, quizId);
                return ControllerSettings.VIEW_QUESTION_LIST;
            }

            populateQuestionListModelFromDto(model, filterDto, page, pageSize, course, type, quizId);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error retrieving questions:", ex);
            populateQuestionListModelFallback(model, page, pageSize, course, type, quizId);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    private QuestionType getQuestionTypeFromString(String type) {
        if (type != null) {
            if (type.equalsIgnoreCase("TRUEFALSE")) type = "TRUEFALSE";
            if (type.equalsIgnoreCase("MULTICHOICE")) type = "MULTICHOICE";

            try {
                return QuestionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid question type provided: {}", type);
                return null;
            }
        }
        return null;
    }

    @GetMapping("/{id:\\d+}")
    public String getQuestionById(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        try {
            QuestionDto question = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id,
                HttpMethod.GET, entity, QuestionDto.class
            ).getBody();

            if (question == null || question.getType() == null) {
                log.error(LOG_QUESTION_NOT_FOUND, id);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, QUESTION_NOT_FOUND);
                return ControllerSettings.VIEW_QUESTION_LIST;
            }

            populateQuestionDetailsModelFromDto(model, question);
            return getQuestionView(question.getType().name());

        } catch (HttpClientErrorException.NotFound ex) {
            log.error(LOG_QUESTION_NOT_FOUND, id);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, QUESTION_NOT_FOUND);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (Exception ex) {
            log.error(ERROR_LOADING_QUESTION + " {}", id, ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ERROR_LOADING_QUESTION);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @PostMapping({"", "/"})
    public String createQuestion(@ModelAttribute QuestionDto questionDto,
                                 @RequestParam("questionType") String questionType,
                                 RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        boolean isUpdate = questionDto.getId() != null;

        try {
            prepareQuestionForSave(questionDto, questionType, isUpdate);

            QuestionDto savedQuestion = isUpdate
                ? updateExistingQuestion(questionDto)
                : createNewQuestion(questionDto);

            if (savedQuestion == null || savedQuestion.getId() == null) {
                throw new IllegalStateException("Failed to save question: API returned null or missing ID");
            }

            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE,
                isUpdate ? "Question updated successfully" : "Question created successfully");

            return REDIRECT_QUESTIONS_PREFIX + savedQuestion.getId();

        } catch (HttpClientErrorException.Forbidden ex) {
            return handleForbiddenError(redirectAttributes);
        } catch (Exception ex) {
            return handleSaveError(ex, redirectAttributes, questionDto.getId());
        }
    }

    /**
     * Prepares question DTO for save/update by setting type, clearing unused fields,
     * setting defaults, and ensuring author exists.
     */
    private void prepareQuestionForSave(QuestionDto questionDto, String questionType, boolean isUpdate) {
        log.info("Preparing question of type: {} (isUpdate: {})", questionType, isUpdate);

        // Set question type
        questionDto.setType(QuestionType.valueOf(questionType.toUpperCase()));

        // Clear multichoice fields for TrueFalse
        if (questionDto.getType() == QuestionType.TRUEFALSE) {
            clearMultichoiceFields(questionDto);
        }

        // Set defaults
        setDefaultValues(questionDto);

        // Validate author (only for new questions)
        if (!isUpdate) {
            ensureAuthorExists(questionDto.getAuthorName());
        }
    }

    /**
     * Clears multichoice-specific fields for TrueFalse questions.
     */
    private void clearMultichoiceFields(QuestionDto questionDto) {
        questionDto.setResponse2(null);
        questionDto.setResponse3(null);
        questionDto.setResponse4(null);
        questionDto.setWeightResponse1(null);
        questionDto.setWeightResponse2(null);
        questionDto.setWeightResponse3(null);
        questionDto.setWeightResponse4(null);
    }

    /**
     * Sets default values for author, course, and quiz if not provided.
     */
    private void setDefaultValues(QuestionDto questionDto) {
        if (questionDto.getAuthorName() == null || questionDto.getAuthorName().isBlank()) {
            questionDto.setAuthorName(ControllerSettings.DEFAULT_AUTHOR);
        }
        if (questionDto.getCourse() == null || questionDto.getCourse().isBlank()) {
            questionDto.setCourse(ControllerSettings.DEFAULT_COURSE);
        }
        if (questionDto.getQuizName() == null || questionDto.getQuizName().isBlank()) {
            questionDto.setQuizName(ControllerSettings.DEFAULT_QUIZ);
        }
    }

    /**
     * Ensures the author exists in the system, creates default author if not found.
     */
    private void ensureAuthorExists(String authorName) {
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_AUTHORS_BY_NAME + authorName,
                HttpMethod.GET,
                entity,
                AuthorDto.class
            );
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Author {} not found, creating default author", authorName);
            createDefaultAuthor();
        } catch (Exception ex) {
            log.warn("Error checking author existence, continuing with save: {}", ex.getMessage());
        }
    }

    /**
     * Creates the default author in the system.
     */
    private void createDefaultAuthor() {
        AuthorDto newAuthor = new AuthorDto();
        newAuthor.setName(ControllerSettings.DEFAULT_AUTHOR);
        newAuthor.setInitials("MM");

        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(newAuthor);
        restTemplate.exchange(
            apiBaseUrl + ControllerSettings.API_AUTHORS,
            HttpMethod.POST,
            entity,
            AuthorDto.class
        );
        log.info("Created default author: {}", ControllerSettings.DEFAULT_AUTHOR);
    }

    /**
     * Updates an existing question via API.
     */
    private QuestionDto updateExistingQuestion(QuestionDto questionDto) {
        log.info("Updating question ID: {}", questionDto.getId());
        HttpEntity<QuestionDto> entity = sessionService.createAuthorizedRequest(questionDto);
        ResponseEntity<QuestionDto> response = restTemplate.exchange(
            apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionDto.getId(),
            HttpMethod.PUT,
            entity,
            QuestionDto.class
        );
        return response.getBody();
    }

    /**
     * Creates a new question via API.
     */
    private QuestionDto createNewQuestion(QuestionDto questionDto) {
        log.info("Creating new question");
        HttpEntity<QuestionDto> entity = sessionService.createAuthorizedRequest(questionDto);
        ResponseEntity<QuestionDto> response = restTemplate.exchange(
            apiBaseUrl + ControllerSettings.API_QUESTIONS,
            HttpMethod.POST,
            entity,
            QuestionDto.class
        );
        return response.getBody();
    }

    /**
     * Handles 403 Forbidden errors by invalidating session and redirecting to login.
     */
    private String handleForbiddenError(RedirectAttributes redirectAttributes) {
        log.error("Session expired while saving question");
        sessionService.invalidateCurrentSession();
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE,
            "Session expired. Please log in again.");
        return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
    }

    /**
     * Handles save errors by logging and setting error message.
     */
    private String handleSaveError(Exception ex, RedirectAttributes redirectAttributes, Long questionId) {
        log.error("Error saving question: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE,
            "Failed to save question: " + ex.getMessage());
        return questionId != null
            ? REDIRECT_QUESTIONS_PREFIX + questionId
            : ControllerSettings.VIEW_REDIRECT_QUESTIONS;
    }

    @DeleteMapping("/{id}")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id, HttpMethod.DELETE, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, "Question deleted successfully");
        return ControllerSettings.VIEW_REDIRECT_QUESTIONS;
    }

    @GetMapping("/quiz/{quizId}")
    public String getQuestionsByQuizId(@PathVariable Long quizId,
                                      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                      Model model) {
        log.info("Listing questions by quiz ID: {}", quizId);
        return renderQuestionList(page, null, null, null, quizId, pageSize, model);
    }

    @GetMapping("/add")
    public String addQuestionForm(@RequestParam("type") String type, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        QuestionDto question = new QuestionDto();
        if (type.equalsIgnoreCase(QuestionType.MULTICHOICE.name())) {
            question.setType(QuestionType.MULTICHOICE);
        } else if (type.equalsIgnoreCase(QuestionType.TRUEFALSE.name())) {
            question.setType(QuestionType.TRUEFALSE);
        }
        model.addAttribute(ControllerSettings.ATTR_QUESTION, question);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return getQuestionView(type);
    }

    @GetMapping("/author/{authorId}/quiz/{quizId}")
    public String getQuestionsByAuthorAndQuiz(@PathVariable Long authorId,
                                              @PathVariable Long quizId,
                                              @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                              Model model) {
        log.info("Listing questions by author ID: {} and quiz ID: {}", authorId, quizId);
        return renderQuestionList(page, null, String.valueOf(authorId), null, quizId, pageSize, model);
    }

    @GetMapping("/{id}/edit")
    public String editQuestion(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        Object loggedInUser = sessionService.getLoggedInUser();
        try {
            QuestionDto question = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id,
                    HttpMethod.GET, entity, QuestionDto.class).getBody();
            AuthorDto[] authors = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS, HttpMethod.GET, entity, AuthorDto[].class).getBody();
            QuizDto[] quizzes = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES, HttpMethod.GET, entity, QuizDto[].class).getBody();
            model.addAttribute(ControllerSettings.ATTR_QUESTION, question);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, authors);
            model.addAttribute(ControllerSettings.ATTR_QUIZZES, quizzes);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            String typeName = (question != null && question.getType() != null) ? question.getType().name() : QuestionType.MULTICHOICE.name();
            return getQuestionView(typeName);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.error("Error loading question {} for edit", id, e);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Unable to load question for edit.");
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    /**
     * Handles question update form submission.
     * Delegates to createQuestion which handles both create and update operations.
     *
     * Note: Transaction management is handled by the REST API layer where
     * database operations occur. This controller only orchestrates API calls.
     */
    @PostMapping("/{id}/edit")
    public String updateQuestion(@PathVariable Long id,
                                 @ModelAttribute QuestionDto questionDto,
                                 @RequestParam(value = "questionType", required = false) String questionType,
                                 RedirectAttributes redirectAttributes) {
        // Ensure ID is set for update operation
        questionDto.setId(id);

        // Determine question type if not provided
        if (questionType == null && questionDto.getType() != null) {
            questionType = questionDto.getType().name();
        } else if (questionType == null) {
            questionType = QuestionType.MULTICHOICE.name(); // Default
        }

        // Delegate to createQuestion which handles both create and update
        return createQuestion(questionDto, questionType, redirectAttributes);
    }

    @GetMapping("/{id:\\d+}/correction")
    public String showQuestionCorrection(@PathVariable("id") Long questionId, Model model) {
        log.info("Showing question correction page, questionId: {}", questionId);
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

            QuestionDto question = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId,
                    HttpMethod.GET, entity, QuestionDto.class
            ).getBody();

            if (question == null) {
                log.error(LOG_QUESTION_NOT_FOUND, questionId);
                model.addAttribute("errorMessage", "Question not found with ID: " + questionId);
                return "question-correction";
            }

            QuestionCorrectionDto correctionDto = new QuestionCorrectionDto();
            correctionDto.setOriginalQuestion(question);
            correctionDto.setLanguage("ro");
            model.addAttribute("correctionDto", correctionDto);

            return "question-correction";

        } catch (HttpClientErrorException.NotFound ex) {
            log.error(LOG_QUESTION_NOT_FOUND, questionId);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, QUESTION_NOT_FOUND);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (Exception ex) {
            log.error(ERROR_LOADING_QUESTION + " {}", questionId, ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ERROR_LOADING_QUESTION);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @PostMapping("/{id:\\d+}/correction/grammar")
    @ResponseBody
    public ResponseEntity<QuestionCorrectionDto> correctGrammar(@PathVariable("id") Long id,
                                                                @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("Processing grammar correction request for question {} via ThyQuestionController", id);
            QuestionCorrectionDto result = correctionService.correctGrammar(correctionDto);
            return ResponseEntity.ok(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Grammar correction interrupted for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error correcting grammar", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}/correction/improve")
    @ResponseBody
    public ResponseEntity<QuestionCorrectionDto> improveQuestion(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("Processing question improvement request for question {} via ThyQuestionController", id);
            QuestionCorrectionDto result = correctionService.improveQuestion(correctionDto);
            return ResponseEntity.ok(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Question improvement interrupted for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error improving question", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}/correction/alternatives")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> generateAlternatives(@PathVariable("id") Long id,
                                                                              @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("Processing generate alternatives request for question {} via ThyQuestionController", id);
            String alternatives = correctionService.generateAlternatives(correctionDto);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("alternatives", alternatives);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Generate alternatives interrupted for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error generating alternatives", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}/correction/explanation")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> explainAnswer(@PathVariable("id") Long id,
                                                                       @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("Processing explain answer request for question {} via ThyQuestionController", id);
            String explanation = correctionService.explainAnswer(correctionDto);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("explanation", explanation);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Explain answer interrupted for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error explaining answer", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}/correction/save")
    @ResponseBody
    public ResponseEntity<QuestionDto> saveImprovedQuestion(@PathVariable("id") Long id,
                                                            @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.info("Saving improved question {} via ThyQuestionController", id);

            // Get the modified question from the correction DTO
            QuestionDto modifiedQuestion = correctionDto.getModifiedQuestion();

            if (modifiedQuestion == null) {
                log.error("Modified question is null in correction DTO");
                return ResponseEntity.badRequest().build();
            }

            // Ensure the ID matches
            modifiedQuestion.setId(id);

            // Call API to update the question
            HttpEntity<QuestionDto> entity = sessionService.createAuthorizedRequest(modifiedQuestion);
            ResponseEntity<QuestionDto> response = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id,
                    HttpMethod.PUT,
                    entity,
                    QuestionDto.class
            );

            log.info("Question {} successfully updated with improved data", id);
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Error saving improved question {}: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Error saving improved question {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Populates model with question details from DTO (for single question view)
     * Similar to populateAuthorDetailsModelFromDto in ThyAuthorController
     */
    private void populateQuestionDetailsModelFromDto(Model model, QuestionDto question) {
        model.addAttribute(ControllerSettings.ATTR_QUESTION, question);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
    }

    /**
     * Populates model with question list data from filter DTO
     */
    private void populateQuestionListModelFromDto(Model model, QuestionFilterDto filterDto,
                                                   Integer page, Integer pageSize,
                                                   String course, String type, Long quizId) {
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, filterDto.getQuestions());
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, filterDto.getAuthors());
        model.addAttribute(ControllerSettings.ATTR_COURSE, filterDto.getCourse());
        model.addAttribute(ControllerSettings.ATTR_AUTHOR_ID, filterDto.getSelectedAuthorId());
        model.addAttribute(ControllerSettings.ATTR_QUIZ, filterDto.getSelectedQuiz());
        model.addAttribute(ControllerSettings.ATTR_QUIZZES, filterDto.getQuizzes() != null ? filterDto.getQuizzes() : new QuizDto[0]);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, filterDto.getCurrentPage() != null ? filterDto.getCurrentPage() : page);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, filterDto.getTotalPages() != null ? filterDto.getTotalPages() : 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, filterDto.getTotalItems() != null ? filterDto.getTotalItems() : 0);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, filterDto.getSelectedCourse() != null ? filterDto.getSelectedCourse() : course);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, filterDto.getSelectedAuthorId());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUIZ_ID, filterDto.getSelectedQuizId() != null ? filterDto.getSelectedQuizId() : quizId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
        model.addAttribute(ControllerSettings.ATTR_COURSES, filterDto.getCourses());
    }

    /**
     * Populates model with fallback/error state for question list
     */
    private void populateQuestionListModelFallback(Model model, Integer page, Integer pageSize,
                                                    String course, String type, Long quizId) {
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, new QuestionDto[0]);
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, new AuthorDto[0]);
        model.addAttribute(ControllerSettings.ATTR_QUIZZES, new QuizDto[0]);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Unexpected error. Please try again later.");
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, course);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUIZ_ID, quizId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
    }
}
