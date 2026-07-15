package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.DuplicateUnlinkRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.thy.service.QuestionCorrectionService;
import com.unitbv.myquiz.thy.service.SessionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Thymeleaf controller for Question management operations.
 * Handles question listing, filtering, creation, editing, and deletion.
 * Supports both multiple choice and true/false question types.
 * Provides server-side rendering for question-related pages.
 */
@Controller
@RequestMapping({ControllerSettings.API_QUESTIONS})
public class ThyQuestionController {
    private static final Logger log = LoggerFactory.getLogger(ThyQuestionController.class);

    private record QuestionNavigationContext(Integer page, Integer pageSize, Long courseId, Long authorId,
                                             String type, Long questionBankId, String backUrl) {
    }

    private final RestTemplate restTemplate;
    private final SessionService sessionService;
    private final QuestionCorrectionService correctionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyQuestionController(SessionService sessionService, RestTemplate restTemplate, QuestionCorrectionService correctionService) {
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
    public String listAllQuestions(@RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
                                   @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_SELECTED_TYPE, required = false) String type,
                                   @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        log.info("Listing all questions");
        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);
        return renderQuestionList(page, courseId, authorId, type, questionBankId, pageSize, model);
    }

    @PostMapping("/filter")
    public String filterQuestions(@RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
                                  @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                  @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                  @RequestParam(value = ControllerSettings.ATTR_SELECTED_TYPE, required = false) String type,
                                  @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                  @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        log.info("Filtering questions");
        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);
        return renderQuestionList(page, courseId, authorId, type, questionBankId, pageSize, model);
    }


    private String renderQuestionList(Integer page, Long courseId, Long authorId, String type, Long questionBankId, Integer pageSize, Model model) {
        log.info("Listing all questions with filters - page: {}, courseId: {}, authorId: {}, type: {}, questionBankId: {}, pageSize: {}", page, courseId, authorId, type, questionBankId, pageSize);

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUESTIONS, new QuestionDto[0]);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, new AuthorDto[0]);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, new QuestionBankDto[0]);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, null);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_LOGIN_REQUIRED_TO_VIEW_QUESTIONS);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }

        if (type != null && type.trim().isEmpty()) {
            type = null;
        }

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        int safePage = pagination.page();
        int safePageSize = pagination.pageSize();

        Object loggedInUser = sessionService.getLoggedInUser();
        try {
            String filterUrl = apiBaseUrl + ControllerSettings.API_QUESTIONS_FILTER;
            QuestionType questionType = getQuestionTypeFromString(type);
            QuestionFilterRequestDto filterInputDto = new QuestionFilterRequestDto();
            filterInputDto.setCourseId(courseId);
            filterInputDto.setAuthorId(authorId);
            filterInputDto.setQuestionType(questionType);
            filterInputDto.setQuestionBank(questionBankId);
            filterInputDto.setPage(safePage);
            filterInputDto.setPageSize(safePageSize);
            log.debug("Filtering questions with input: {}", filterInputDto);

            HttpEntity<QuestionFilterRequestDto> requestEntity = sessionService.createAuthorizedRequest(filterInputDto);

            ResponseEntity<QuestionFilterResponseDto> response = restTemplate.exchange(filterUrl, HttpMethod.POST, requestEntity, QuestionFilterResponseDto.class);
            QuestionFilterResponseDto filterDto = response.getBody();
            if (filterDto == null) {
                log.error("API returned null for QuestionFilterResponseDto");
                populateQuestionListModelFallback(model, safePage, safePageSize, courseId, type, questionBankId);
                return ControllerSettings.VIEW_QUESTION_LIST;
            }

            populateQuestionListModelFromDto(model, filterDto, safePage, safePageSize, type);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error retrieving questions:", ex);
            populateQuestionListModelFallback(model, safePage, safePageSize, courseId, type, questionBankId);
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
    public String getQuestionById(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                   @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                   @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_SELECTED_TYPE, required = false) String type,
                                   @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                   @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);

        try {
            QuestionDto question = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id, HttpMethod.GET, entity, QuestionDto.class).getBody();

            if (question == null || question.getType() == null) {
                log.error(ControllerSettings.LOG_QUESTION_NOT_FOUND, id);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
                return ControllerSettings.VIEW_QUESTION_LIST;
            }

            QuestionBankDto[] questionBankDtos = fetchAllQuestionBanks(entity);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, questionBankDtos);

            QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
            String resolvedBackUrl = resolveBackToQuestionsUrl(navigationContext);

            populateQuestionDetailsModelFromDto(model, question);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTIONS_URL, resolvedBackUrl);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_EDIT_URL, buildQuestionEditUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_DUPLICATES_URL, buildQuestionDuplicatesUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_CORRECTION_URL, buildQuestionCorrectionUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, navigationContext.page());
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, navigationContext.pageSize());
            return ControllerSettings.VIEW_QUESTION_VIEW;

        } catch (HttpClientErrorException.NotFound ex) {
            log.error(ControllerSettings.LOG_QUESTION_NOT_FOUND, id);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (Exception ex) {
            log.error(ControllerSettings.MSG_ERROR_LOADING_QUESTION + " {}", id, ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_LOADING_QUESTION);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @PostMapping({"", "/"})
    public String createQuestion(@ModelAttribute QuestionDto questionDto, @RequestParam("questionType") String questionType, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        boolean isUpdate = questionDto.getId() != null;

        try {
            prepareQuestionForSave(questionDto, questionType, isUpdate);

            QuestionDto savedQuestion = isUpdate ? updateExistingQuestion(questionDto) : createNewQuestion(questionDto);

            if (savedQuestion == null || savedQuestion.getId() == null) {
                throw new IllegalStateException("Failed to save question: API returned null or missing ID");
            }

            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE,
                    isUpdate ? ControllerSettings.MSG_QUESTION_UPDATED_SUCCESS : ControllerSettings.MSG_QUESTION_CREATED_SUCCESS);

            return ControllerSettings.REDIRECT_QUESTIONS_PREFIX + savedQuestion.getId();

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
            ensureAuthorExists(questionDto.getAuthor() != null ? questionDto.getAuthor().getName() : null);
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
     * Sets default values for author, course, and question_bank if not provided.
     */
    private void setDefaultValues(QuestionDto questionDto) {
        if (questionDto.getAuthor() == null || questionDto.getAuthor().getName() == null || questionDto.getAuthor().getName().isBlank()) {
            questionDto.setAuthor(AuthorInfo.builder()
                    .name(ControllerSettings.DEFAULT_AUTHOR)
                    .initials(ControllerSettings.DEFAULT_AUTHOR)
                    .build());
        }
        if (questionDto.getCourse() == null || questionDto.getCourse().isBlank()) {
            questionDto.setCourse(ControllerSettings.DEFAULT_COURSE);
        }
        if (questionDto.getQuestionBankName() == null || questionDto.getQuestionBankName().isBlank()) {
            questionDto.setQuestionBankName(ControllerSettings.DEFAULT_QUESTION_BANK);
        }
    }

    /**
     * Ensures the author exists in the system, creates default author if not found.
     */
    private void ensureAuthorExists(String authorName) {
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String authorLookupUrl = UriComponentsBuilder
                    .fromUriString(apiBaseUrl + ControllerSettings.API_AUTHORS_BY_NAME)
                    .pathSegment(authorName)
                    .build()
                    .encode()
                    .toUriString();
            restTemplate.exchange(authorLookupUrl, HttpMethod.GET, entity, AuthorDto.class);
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
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS, HttpMethod.POST, entity, AuthorDto.class);
        log.info("Created default author: {}", ControllerSettings.DEFAULT_AUTHOR);
    }

    /**
     * Updates an existing question via API.
     */
    private QuestionDto updateExistingQuestion(QuestionDto questionDto) {
        log.info("Updating question ID: {}", questionDto.getId());
        HttpEntity<QuestionDto> entity = sessionService.createAuthorizedRequest(questionDto);
        ResponseEntity<QuestionDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionDto.getId(), HttpMethod.PUT, entity, QuestionDto.class);
        return response.getBody();
    }

    /**
     * Creates a new question via API.
     */
    private QuestionDto createNewQuestion(QuestionDto questionDto) {
        log.info("Creating new question");
        HttpEntity<QuestionDto> entity = sessionService.createAuthorizedRequest(questionDto);
        ResponseEntity<QuestionDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS, HttpMethod.POST, entity, QuestionDto.class);
        return response.getBody();
    }

    /**
     * Handles 403 Forbidden errors by invalidating session and redirecting to login.
     */
    private String handleForbiddenError(RedirectAttributes redirectAttributes) {
        log.error("Session expired while saving question");
        sessionService.invalidateCurrentSession();
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
        return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
    }

    /**
     * Handles save errors by logging and setting error message.
     */
    private String handleSaveError(Exception ex, RedirectAttributes redirectAttributes, Long questionId) {
        log.error("Error saving question: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SAVE_QUESTION_FAILED_PREFIX + ex.getMessage());
        return questionId != null ? ControllerSettings.REDIRECT_QUESTIONS_PREFIX + questionId : ControllerSettings.VIEW_REDIRECT_QUESTIONS;
    }

    @DeleteMapping("/{id}")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id, HttpMethod.DELETE, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_QUESTION_DELETED_SUCCESS);
        return ControllerSettings.VIEW_REDIRECT_QUESTIONS;
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
        QuestionBankDto[] questionBankDtos = fetchAllQuestionBanks(sessionService.getAuthorizationHeader());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, questionBankDtos);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return getQuestionView(type);
    }

    @GetMapping({ControllerSettings.API_QUESTION_BANKS_GET_BY_ID_AND_AUTHOR_ID})
    public String getQuestionsByAuthorAndQuestionBank(@PathVariable Long authorId, @PathVariable Long questionBankId,
                                                      @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
                                                      @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        log.info("Listing questions by author ID: {} and questionBank ID: {}", authorId, questionBankId);
        return renderQuestionList(page, null, authorId, null, questionBankId, pageSize, model);
    }

    @GetMapping("/sample")
    @ResponseBody
    public ResponseEntity<QuestionDto> getSampleQuestion(@RequestParam(value = "type", defaultValue = "MULTICHOICE") String type) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String sampleUrl = UriComponentsBuilder
                    .fromUriString(apiBaseUrl + ControllerSettings.API_QUESTIONS + ControllerSettings.API_QUESTION_SAMPLE)
                    .queryParam("type", type)
                    .toUriString();

            ResponseEntity<QuestionDto> response = restTemplate.exchange(sampleUrl, HttpMethod.GET, entity, QuestionDto.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException ex) {
            log.warn("Failed to load sample question for type {}: {}", type, ex.getStatusCode());
            return ResponseEntity.status(ex.getStatusCode()).build();
        } catch (Exception ex) {
            log.error("Unexpected error while loading sample question for type {}", type, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/edit")
    public String editQuestion(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                               @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                               @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        Object loggedInUser = sessionService.getLoggedInUser();
        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);

        try {
            QuestionDto question = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id, HttpMethod.GET, entity, QuestionDto.class).getBody();
            AuthorDto[] authors = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS, HttpMethod.GET, entity, AuthorDto[].class).getBody();
            QuestionBankDto[] questionBankDtos = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS, HttpMethod.GET, entity, QuestionBankDto[].class).getBody();
            model.addAttribute(ControllerSettings.ATTR_QUESTION, question);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, authors);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, questionBankDtos);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTIONS_URL, resolveBackToQuestionsUrl(navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_DUPLICATES_URL, buildQuestionDuplicatesUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_CORRECTION_URL, buildQuestionCorrectionUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_VIEW_URL, buildQuestionViewUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, navigationContext.page());
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, navigationContext.pageSize());
            String typeName = (question != null && question.getType() != null) ? question.getType().name() : QuestionType.MULTICHOICE.name();
            return getQuestionView(typeName);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.error("Error loading question {} for edit", id, e);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_UNABLE_LOAD_QUESTION_FOR_EDIT);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @GetMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_DUPLICATES)
    public String showQuestionDuplicates(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                         @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                         @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                         @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                         @RequestParam(value = "type", required = false) String type,
                                         @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                         @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl, Model model) {
        log.info("Loading duplicates for question id {}", id);
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
            Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
            Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            QuestionDto question = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id + "/duplicates", HttpMethod.GET, entity,
                    QuestionDto.class
            ).getBody();

            log.info("Loaded question id {}, with no of duplicates {}", id,
                     question != null ? question.getDuplicates().size() : "N/A");

            if (question == null) {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
                return ControllerSettings.VIEW_QUESTION_LIST;
            }

            QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
            String resolvedBackUrl = resolveBackToQuestionsUrl(navigationContext);

            List<?> allDuplicates = question.getDuplicates() != null ? question.getDuplicates() : new ArrayList<>();
            int totalElements = allDuplicates.size();
            int pageSizeValue = navigationContext.pageSize();
            int totalPages = Math.max(1, (int) Math.ceil(totalElements / (double) pageSizeValue));
            int currentPage = Math.min(Math.max(navigationContext.page(), 1), totalPages);
            int fromIndex = Math.min((currentPage - 1) * pageSizeValue, totalElements);
            int toIndex = Math.min(fromIndex + pageSizeValue, totalElements);
            List<?> pagedDuplicates = allDuplicates.subList(fromIndex, toIndex);

            model.addAttribute(ControllerSettings.ATTR_QUESTION, question);
            model.addAttribute(ControllerSettings.ATTR_DUPLICATES, pagedDuplicates);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, currentPage);
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSizeValue);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, totalPages);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, (long) totalElements);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTIONS_URL, resolvedBackUrl);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_VIEW_URL, buildQuestionViewUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_CORRECTION_URL, buildQuestionCorrectionUrl(id, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            log.debug("Duplicates pagination for question {}: requestedPage={}, normalizedPage={}, pageSize={}, totalElements={}, totalPages={}",
                    id, page, currentPage, pageSizeValue, totalElements, totalPages);
            return ControllerSettings.VIEW_QUESTION_DUPLICATES;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired while loading duplicates for question {}", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.NotFound ex) {
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (Exception ex) {
            log.error("Error loading duplicates for question {}", id, ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_UNABLE_LOAD_DUPLICATES);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @PostMapping(ControllerSettings.API_QUESTION_BANKS_DUPLICATES_REMOVE_BY_ID)
    public String removeSelectedDuplicates(@PathVariable Long id,
                                           @RequestParam(value = ControllerSettings.ATTR_DUPLICATE_QUESTION_IDS, required = false) List<Long> duplicateQuestionIds,
                                           @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                           @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                           @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                           @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                           @RequestParam(value = "type", required = false) String type,
                                           @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                           @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl,
                                           RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);

        if (duplicateQuestionIds == null || duplicateQuestionIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SELECT_AT_LEAST_ONE_DUPLICATE);
            QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
            return buildQuestionDuplicatesRedirectUrl(id, navigationContext);
        }

        log.info("Removing {} selected duplicates for question {}", duplicateQuestionIds.size(), id);
        try {
            DuplicateUnlinkRequestDto selectionDto = new DuplicateUnlinkRequestDto();
            selectionDto.setDuplicateQuestionIds(duplicateQuestionIds);
            HttpEntity<DuplicateUnlinkRequestDto> entity = sessionService.createAuthorizedRequest(selectionDto);
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id + ControllerSettings.API_QUESTION_DUPLICATES_REMOVE, HttpMethod.POST, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_DUPLICATES_REMOVED_SUCCESS);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired while removing duplicates for question {}", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error removing duplicates for question {}", id, ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_DUPLICATES_REMOVED_FAILED);
        }

        QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
        return buildQuestionDuplicatesRedirectUrl(id, navigationContext);
    }

    @PostMapping(ControllerSettings.API_QUESTION_BANKS_DUPLICATES_REMOVE_ALL_BY_ID)
    public String removeAllDuplicates(@PathVariable Long id,
                                      @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                      @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                      @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) String courseIdParam,
                                      @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) String authorIdParam,
                                      @RequestParam(value = "type", required = false) String type,
                                      @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) String questionBankIdParam,
                                      @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl,
                                      RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
        Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
        Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);

        log.info("Removing all duplicates for question {}", id);
        try {
            HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id + ControllerSettings.API_QUESTION_DUPLICATES_REMOVE_ALL, HttpMethod.POST, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_ALL_DUPLICATES_REMOVED_SUCCESS);
            log.info("Successfully removed all duplicates for question {}", id);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired while removing all duplicates for question {}", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Question not found while removing all duplicates: {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
        } catch (Exception ex) {
            log.error("Error removing all duplicates for question {}", id, ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ALL_DUPLICATES_REMOVED_FAILED);
        }

        QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
        return buildQuestionDuplicatesRedirectUrl(id, navigationContext);
    }


    @GetMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION)
    public String showQuestionCorrection(@PathVariable("id") Long questionId, @RequestParam(value = "page", required = false) Integer page,
                                         @RequestParam(value = "pageSize", required = false) Integer pageSize, @RequestParam(value = "courseId", required = false) String courseIdParam,
                                         @RequestParam(value = "authorId", required = false) String authorIdParam, @RequestParam(value = "type", required = false) String type,
                                          @RequestParam(value = "questionBankId", required = false) String questionBankIdParam,
                                          @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl, Model model) {
        log.info("Showing question correction page, questionId: {}", questionId);
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            Long courseId = parseOptionalLong(courseIdParam, ControllerSettings.ATTR_COURSE_ID);
            Long authorId = parseOptionalLong(authorIdParam, ControllerSettings.ATTR_AUTHOR_ID);
            Long questionBankId = parseOptionalLong(questionBankIdParam, ControllerSettings.ATTR_QUESTION_BANK_ID);
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

            QuestionDto question = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + questionId, HttpMethod.GET, entity, QuestionDto.class).getBody();

            if (question == null) {
                log.error(ControllerSettings.LOG_QUESTION_NOT_FOUND, questionId);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND_WITH_ID_PREFIX + questionId);
                return ControllerSettings.VIEW_QUESTION_CORRECTION;
            }

            QuestionCorrectionDto correctionDto = new QuestionCorrectionDto();
            correctionDto.setOriginalQuestion(question);
            correctionDto.setLanguage(ControllerSettings.DEFAULT_CORRECTION_LANGUAGE);
            model.addAttribute(ControllerSettings.ATTR_CORRECTION_DTO, correctionDto);
            QuestionNavigationContext navigationContext = buildNavigationContext(page, pageSize, courseId, authorId, type, questionBankId, backUrl);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTIONS_URL, resolveBackToQuestionsUrl(navigationContext));
            model.addAttribute(ControllerSettings.ATTR_QUESTION_VIEW_URL, buildQuestionViewUrl(questionId, navigationContext));
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, navigationContext.page());
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, navigationContext.pageSize());
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());

            return ControllerSettings.VIEW_QUESTION_CORRECTION;

        } catch (HttpClientErrorException.NotFound ex) {
            log.error(ControllerSettings.LOG_QUESTION_NOT_FOUND, questionId);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_QUESTION_NOT_FOUND);
            return ControllerSettings.VIEW_QUESTION_LIST;
        } catch (Exception ex) {
            log.error(ControllerSettings.MSG_ERROR_LOADING_QUESTION + " {}", questionId, ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_LOADING_QUESTION);
            return ControllerSettings.VIEW_QUESTION_LIST;
        }
    }

    @PostMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION_GRAMMAR)
    @ResponseBody
    public ResponseEntity<QuestionCorrectionDto> correctGrammar(@PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session during grammar correction for question {}", id);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("Processing grammar correction request for question {} via ThyQuestionController", id);
            QuestionCorrectionDto result = correctionService.correctGrammar(correctionDto);
            return ResponseEntity.ok(result);
        } catch (QuestionCorrectionService.CorrectionServiceException e) {
            log.error(ControllerSettings.LOG_CORRECTION_SERVICE_ERROR, id, e.getMessage());
            return ResponseEntity.status(resolveCorrectionErrorStatus(e)).build();
        } catch (Exception e) {
            log.error("Unexpected error correcting grammar for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION_IMPROVE)
    @ResponseBody
    public ResponseEntity<QuestionCorrectionDto> improveQuestion(@PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session during question improvement for question {}", id);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("Processing question improvement request for question {} via ThyQuestionController", id);
            QuestionCorrectionDto result = correctionService.improveQuestion(correctionDto);
            return ResponseEntity.ok(result);
        } catch (QuestionCorrectionService.CorrectionServiceException e) {
            log.error(ControllerSettings.LOG_CORRECTION_SERVICE_ERROR, id, e.getMessage());
            return ResponseEntity.status(resolveCorrectionErrorStatus(e)).build();
        } catch (Exception e) {
            log.error("Unexpected error improving question {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION_ALTERNATIVES)
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> generateAlternatives(@PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session during generate alternatives for question {}", id);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("Processing generate alternatives request for question {} via ThyQuestionController", id);
            String alternatives = correctionService.generateAlternatives(correctionDto);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put(ControllerSettings.RESPONSE_KEY_ALTERNATIVES, alternatives);
            response.put(ControllerSettings.RESPONSE_KEY_STATUS, ControllerSettings.RESPONSE_VALUE_SUCCESS);
            return ResponseEntity.ok(response);
        } catch (QuestionCorrectionService.CorrectionServiceException e) {
            log.error(ControllerSettings.LOG_CORRECTION_SERVICE_ERROR, id, e.getMessage());
            return ResponseEntity.status(resolveCorrectionErrorStatus(e)).build();
        } catch (Exception e) {
            log.error("Unexpected error generating alternatives for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION_EXPLANATION)
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> explainAnswer(@PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session during explain answer for question {}", id);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("Processing explain answer request for question {} via ThyQuestionController", id);
            String explanation = correctionService.explainAnswer(correctionDto);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put(ControllerSettings.RESPONSE_KEY_EXPLANATION, explanation);
            response.put(ControllerSettings.RESPONSE_KEY_STATUS, ControllerSettings.RESPONSE_VALUE_SUCCESS);
            return ResponseEntity.ok(response);
        } catch (QuestionCorrectionService.CorrectionServiceException e) {
            log.error(ControllerSettings.LOG_CORRECTION_SERVICE_ERROR, id, e.getMessage());
            return ResponseEntity.status(resolveCorrectionErrorStatus(e)).build();
        } catch (Exception e) {
            log.error("Unexpected error explaining answer for question {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id:\\d+}" + ControllerSettings.API_QUESTION_CORRECTION_SAVE)
    @ResponseBody
    public ResponseEntity<QuestionDto> saveImprovedQuestion(@PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session during save improved question for question {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
            ResponseEntity<QuestionDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTIONS + "/" + id, HttpMethod.PUT, entity, QuestionDto.class);

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

    private QuestionBankDto[] fetchAllQuestionBanks(HttpEntity<Void> entity) {
        try {
            QuestionBankDto[] questionBankDtos = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS, HttpMethod.GET, entity, QuestionBankDto[].class).getBody();
            return questionBankDtos != null ? questionBankDtos : new QuestionBankDto[0];
        } catch (Exception e) {
            log.warn("Unable to fetch questionBanks for question editor", e);
            return new QuestionBankDto[0];
        }
    }

    /**
     * Populates model with question list data from filter DTO.
     * Ensures all required filter dropdown data is included.
     */
    private void populateQuestionListModelFromDto(Model model, QuestionFilterResponseDto filterDto, Integer page, Integer pageSize, String type) {
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        int currentPage = filterDto.getPage() != null ? filterDto.getPage() : pagination.page();
        int effectivePageSize = filterDto.getPageSize() != null ? filterDto.getPageSize() : pagination.pageSize();

        // Questions and pagination
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, filterDto.getQuestions() != null ? filterDto.getQuestions() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, currentPage);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, filterDto.getTotalPages() != null ? filterDto.getTotalPages() : 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, filterDto.getTotalElements() != null ? filterDto.getTotalElements() : 0);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, effectivePageSize);

        // Filter selections
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, filterDto.getSelectedCourse());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, filterDto.getSelectedCourseId());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, filterDto.getSelectedAuthorId());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, filterDto.getSelectedQuestionBankId());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);

        // Filter dropdowns (from service)
        model.addAttribute(ControllerSettings.ATTR_COURSES, filterDto.getAllCourses() != null ? filterDto.getAllCourses() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, filterDto.getAuthors() != null ? filterDto.getAuthors() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, filterDto.getQuestionBanks() != null ? filterDto.getQuestionBanks() : new ArrayList<>());
    }

    /**
     * Populates model with fallback/error state for question list
     */
    private void populateQuestionListModelFallback(Model model, Integer page, Integer pageSize, Long courseId, String type, Long questionBankId) {
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, new QuestionDto[0]);
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_UNEXPECTED_ERROR_RETRY_LATER);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, pagination.page());
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_TYPE, type);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pagination.pageSize());
        model.addAttribute(ControllerSettings.ATTR_COURSES, new ArrayList<>());
    }

    private String buildQuestionsBackUrl(Integer page, Integer pageSize, Long courseId, Long authorId, String type, Long questionBankId) {
        StringBuilder url = new StringBuilder("/questions");
        String separator = "?";

        if (courseId != null) {
            url.append(separator).append(ControllerSettings.ATTR_COURSE_ID).append("=").append(courseId);
            separator = "&";
        }
        if (questionBankId != null) {
            url.append(separator).append(ControllerSettings.ATTR_QUESTION_BANK_ID).append("=").append(questionBankId);
            separator = "&";
        }
        if (authorId != null) {
            url.append(separator).append("authorId=").append(authorId);
            separator = "&";
        }
        if (type != null && !type.isBlank()) {
            url.append(separator).append("type=").append(URLEncoder.encode(type.trim(), StandardCharsets.UTF_8));
            separator = "&";
        }
        if (page != null && page > 0) {
            url.append(separator).append("page=").append(page);
            separator = "&";
        }
        if (pageSize != null && pageSize > 0) {
            url.append(separator).append("pageSize=").append(pageSize);
        }

        return url.toString();
    }

    private String buildQuestionEditUrl(Long questionId, QuestionNavigationContext navigationContext) {
        String base = ControllerSettings.QUESTIONS_PATH_PREFIX + questionId + ControllerSettings.API_QUESTION_EDIT;
        return appendQuestionContext(base, navigationContext);
    }

    private String buildQuestionViewUrl(Long questionId, QuestionNavigationContext navigationContext) {
        String base = ControllerSettings.QUESTIONS_PATH_PREFIX + questionId;
        return appendQuestionContext(base, navigationContext);
    }

    private String buildQuestionDuplicatesUrl(Long questionId, QuestionNavigationContext navigationContext) {
        String base = ControllerSettings.QUESTIONS_PATH_PREFIX + questionId + ControllerSettings.API_QUESTION_DUPLICATES;
        return appendQuestionContext(base, navigationContext);
    }

    private String buildQuestionDuplicatesRedirectUrl(Long questionId, QuestionNavigationContext navigationContext) {
        return "redirect:" + buildQuestionDuplicatesUrl(questionId, navigationContext);
    }

    private String buildQuestionCorrectionUrl(Long questionId, QuestionNavigationContext navigationContext) {
        String base = ControllerSettings.QUESTIONS_PATH_PREFIX + questionId + ControllerSettings.API_QUESTION_CORRECTION;
        return appendQuestionContext(base, navigationContext);
    }

    private Long parseOptionalLong(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid value for request parameter '" + paramName + "': " + value, ex);
        }
    }

    private QuestionNavigationContext buildNavigationContext(Integer page, Integer pageSize, Long courseId, Long authorId,
                                                             String type, Long questionBankId, String backUrl) {
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        return new QuestionNavigationContext(pagination.page(), pagination.pageSize(), courseId, authorId, type, questionBankId, backUrl);
    }

    private HttpStatus resolveCorrectionErrorStatus(QuestionCorrectionService.CorrectionServiceException e) {
        Throwable cause = e.getCause();
        if (cause instanceof HttpClientErrorException httpClientErrorException) {
            return HttpStatus.valueOf(httpClientErrorException.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String appendQuestionListContext(String baseUrl, Integer page, Integer pageSize, Long courseId, Long authorId, String type, Long questionBankId) {
        String backUrl = buildQuestionsBackUrl(page, pageSize, courseId, authorId, type, questionBankId);
        if (!backUrl.contains("?")) {
            return baseUrl;
        }
        return baseUrl + "?" + backUrl.substring(backUrl.indexOf('?') + 1);
    }

    private String appendQuestionContext(String baseUrl, QuestionNavigationContext navigationContext) {
        String normalizedBackUrl = normalizeInternalBackUrl(navigationContext.backUrl());
        if (normalizedBackUrl != null) {
            return UriComponentsBuilder.fromPath(baseUrl)
                    .queryParam(ControllerSettings.ATTR_BACK_URL, normalizedBackUrl)
                    .build()
                    .encode()
                    .toUriString();
        }
        return appendQuestionListContext(baseUrl, navigationContext.page(), navigationContext.pageSize(), navigationContext.courseId(),
                navigationContext.authorId(), navigationContext.type(), navigationContext.questionBankId());
    }

    private String resolveBackToQuestionsUrl(QuestionNavigationContext navigationContext) {
        String normalizedBackUrl = normalizeInternalBackUrl(navigationContext.backUrl());
        return normalizedBackUrl != null ? normalizedBackUrl : buildQuestionsBackUrl(
                navigationContext.page(),
                navigationContext.pageSize(),
                navigationContext.courseId(),
                navigationContext.authorId(),
                navigationContext.type(),
                navigationContext.questionBankId());
    }

    private String normalizeInternalBackUrl(String backUrl) {
        if (backUrl == null || backUrl.isBlank()) {
            return null;
        }

        String trimmedBackUrl = backUrl.trim();
        if (trimmedBackUrl.startsWith("http://") || trimmedBackUrl.startsWith("https://") || trimmedBackUrl.startsWith("//")) {
            log.warn("Ignoring external backUrl: {}", trimmedBackUrl);
            return null;
        }

        return trimmedBackUrl.startsWith("/") ? trimmedBackUrl : "/" + trimmedBackUrl;
    }
}
