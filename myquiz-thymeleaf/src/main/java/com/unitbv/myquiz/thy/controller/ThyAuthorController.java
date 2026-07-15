package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.AuthorFormDataDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.thy.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Thymeleaf controller for Author management operations.
 * Handles author listing, filtering, creation, editing, and deletion.
 */
@Controller
@RequestMapping("/authors")
public class ThyAuthorController {

    private static final Logger log = LoggerFactory.getLogger(ThyAuthorController.class);
    private final RestTemplate restTemplate;
    private final SessionService sessionService;
    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyAuthorController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    // ─── Author list rendering ─────────────────────────────────────────────────

    @GetMapping(
            {
                    "/",
                    ""
            }
    )
    public String listAuthors(
            @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page, @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model
    ) {
        return renderAuthorList(
                courseId,
                authorId,
                questionBankId,
                page,
                pageSize,
                model
        );
    }

    @GetMapping(
            {
                    "/course/{course}/page/{page}",
                    "/course/{course}",
                    "/page/{page}"
            }
    )
    public String listAuthorsByCourseAndPage(
            @PathVariable(value = "course", required = false) String course, @PathVariable(value = "page", required = false) Integer page,
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model
    ) {
        // `course` path variable exists for URL compatibility; courseId filtering is not applied here
        log.debug(
                "listAuthorsByCourseAndPage: course={}, page={}",
                course,
                page
        );
        return renderAuthorList(
                null,
                authorId,
                questionBankId,
                page,
                pageSize,
                model
        );
    }

    @GetMapping("/filter")
    @PostMapping("/filter")
    public String filterAuthorsByCourse(
            @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page, @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model
    ) {
        return renderAuthorList(
                courseId,
                authorId,
                questionBankId,
                page,
                pageSize,
                model
        );
    }

    private String renderAuthorList(Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        PaginationParams pagination = PaginationSupport.normalize(
                page,
                pageSize
        );
        AuthorFilterRequestDto filterInput = buildAuthorFilterRequest(
                courseId,
                authorId,
                questionBankId,
                pagination
        );
        log.debug(
                "Rendering author list with filter: {}",
                filterInput
        );

        AuthorFilterResponseDto filterDto;
        try {
            filterDto = fetchFilteredAuthors(filterInput);
        }
        catch (RuntimeException ex) {
            log.error(
                    "Redirecting to login due to session error: {}",
                    ex.getMessage()
            );
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }

        populateAuthorListModel(
                model,
                filterDto,
                courseId,
                authorId,
                pagination
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
        return ControllerSettings.VIEW_AUTHOR_LIST;
    }

    /**
     * Builds author filter request DTO from the given filter parameters and pagination.
     */
    private AuthorFilterRequestDto buildAuthorFilterRequest(Long courseId, Long authorId, Long questionBankId, PaginationParams pagination) {
        AuthorFilterRequestDto filterInput = new AuthorFilterRequestDto();
        filterInput.setCourseId(courseId);
        filterInput.setPage(pagination.page());
        filterInput.setPageSize(pagination.pageSize());
        filterInput.setAuthorId(authorId);
        filterInput.setQuestionBankId(questionBankId);
        return filterInput;
    }

    /**
     * Populates model with author list data from filter DTO, or falls back to empty model.
     * Delegates to {@link #populateModelFromFilterResponse} or {@link #populateEmptyAuthorListModel}.
     */
    private void populateAuthorListModel(Model model, AuthorFilterResponseDto filterDto, Long courseId, Long authorId, PaginationParams pagination) {
        if (filterDto != null) {
            populateModelFromFilterResponse(
                    model,
                    filterDto,
                    authorId,
                    pagination
            );
        } else {
            populateEmptyAuthorListModel(
                    model,
                    courseId,
                    authorId,
                    pagination
            );
        }
    }

    /**
     * Populates model with author list data from filter response DTO.
     * Ensures all required filter dropdown data is included.
     */
    private void populateModelFromFilterResponse(Model model, AuthorFilterResponseDto filterDto, Long authorId, PaginationParams pagination) {
        model.addAttribute(
                ControllerSettings.ATTR_AUTHORS,
                filterDto.getAuthors() != null ? filterDto.getAuthors() : new ArrayList<>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_COURSES,
                filterDto.getCourses() != null ? filterDto.getCourses() : new ArrayList<>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE,
                filterDto.getSelectedCourse()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE_ID,
                filterDto.getSelectedCourseId()
        );
        model.addAttribute(
                ControllerSettings.ATTR_CURRENT_PAGE,
                filterDto.getPage() != null ? filterDto.getPage() : pagination.page()
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_PAGES,
                filterDto.getTotalPages() != null ? filterDto.getTotalPages() : 1
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_ELEMENTS,
                filterDto.getTotalElements() != null ? filterDto.getTotalElements() : 0L
        );
        model.addAttribute(
                ControllerSettings.ATTR_PAGE_SIZE,
                filterDto.getPageSize() != null ? filterDto.getPageSize() : pagination.pageSize()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_AUTHOR_ID,
                authorId
        );
        model.addAttribute(
                ControllerSettings.ATTR_AUTHOR_LIST,
                filterDto.getAuthorOptions()
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTION_BANKS,
                filterDto.getQuestionBanks()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID,
                filterDto.getSelectedQuestionBankId()
        );
    }

    /**
     * Populates model with fallback/error state for author list.
     */
    private void populateEmptyAuthorListModel(Model model, Long courseId, Long authorId, PaginationParams pagination) {
        model.addAttribute(
                ControllerSettings.ATTR_AUTHORS,
                new ArrayList<>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_COURSES,
                new ArrayList<>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE_ID,
                courseId
        );
        model.addAttribute(
                ControllerSettings.ATTR_CURRENT_PAGE,
                pagination.page()
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_PAGES,
                0
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_ELEMENTS,
                0L
        );
        model.addAttribute(
                ControllerSettings.ATTR_PAGE_SIZE,
                pagination.pageSize()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_AUTHOR_ID,
                authorId
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTION_BANKS,
                null
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID,
                null
        );
    }

    /**
     * Fetches filtered authors from the API using the given filter request.
     * Throws {@link SecurityException} if session is expired (403 Forbidden).
     */
    private AuthorFilterResponseDto fetchFilteredAuthors(AuthorFilterRequestDto filter) {
        log.debug(
                "Fetching filtered authors: {}",
                filter
        );
        String url = apiBaseUrl + ControllerSettings.API_AUTHORS_FILTER;
        HttpEntity<AuthorFilterRequestDto> requestEntity = sessionService.createAuthorizedRequest(filter);
        try {
            ResponseEntity<AuthorFilterResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AuthorFilterResponseDto.class
            );
            return response.getBody();
        }
        catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token may be invalid or expired. Redirecting to login");
            sessionService.invalidateCurrentSession();
            throw new SecurityException("Session expired or insufficient permissions. Please log in again.");
        }
        catch (Exception ex) {
            log.error(
                    "Error filtering authors: {}",
                    ex.getMessage(),
                    ex
            );
            return null;
        }
    }

    // ─── Author lookup by name ─────────────────────────────────────────────────

    @GetMapping("/author/{name}")
    public String getAuthorByName(@PathVariable String name, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = UriComponentsBuilder.fromUriString(apiBaseUrl + ControllerSettings.API_AUTHORS_BY_NAME).pathSegment(name).path(ControllerSettings.API_AUTHORS_QUESTIONS_SUFFIX).build().encode()
                                         .toUriString();
        ResponseEntity<AuthorFormDataDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                AuthorFormDataDto.class
        );
        prepareAuthorDataModelAttributes(
                model,
                response.getBody()
        );
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    /**
     * Prepares author form data model attributes for the question list view.
     */
    private void prepareAuthorDataModelAttributes(Model model, AuthorFormDataDto authorData) {
        if (authorData != null && authorData.getAuthor() != null && authorData.getQuestionBankDtos() != null && !authorData.getQuestionBankDtos().isEmpty()) {
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    authorData.getAuthor()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHORS,
                    authorData.getAuthorsList()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_QUESTION_BANKS,
                    authorData.getQuestionBankDtos()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_COURSE,
                    authorData.getQuestionBankDtos().getFirst().getCourse()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_SELECTED_AUTHOR_ID,
                    authorData.getAuthor().getId()
            );
        } else {
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    null
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHORS,
                    null
            );
            model.addAttribute(
                    ControllerSettings.ATTR_QUESTION_BANKS,
                    null
            );
            model.addAttribute(
                    ControllerSettings.ATTR_COURSE,
                    null
            );
            model.addAttribute(
                    ControllerSettings.ATTR_SELECTED_AUTHOR_ID,
                    null
            );
        }
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
    }

    // ─── Questions by author ───────────────────────────────────────────────────

    @PostMapping("/author/id/questions")
    public String getQuestionsByAuthor(
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String selAuthorId, @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page, @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model
    ) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info(
                "Fetching questions for author ID: {} and courseId: {}",
                selAuthorId,
                courseId
        );
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        if (questionBankId != null) {
            QuestionBankDto questionBankDto = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + questionBankId,
                    HttpMethod.GET,
                    entity,
                    QuestionBankDto.class
            ).getBody();
            model.addAttribute(
                    ControllerSettings.ATTR_QUESTION_BANK,
                    questionBankDto
            );
        }

        PaginationParams pagination = PaginationSupport.normalize(
                page,
                pageSize
        );
        Long normalizedAuthorId = parseAuthorId(selAuthorId);

        QuestionFilterRequestDto filterInput = new QuestionFilterRequestDto();
        filterInput.setAuthorId(normalizedAuthorId);
        filterInput.setCourseId(courseId);
        filterInput.setQuestionBank(questionBankId);
        filterInput.setPage(pagination.page());
        filterInput.setPageSize(pagination.pageSize());

        HttpEntity<QuestionFilterRequestDto> filterEntity = sessionService.createAuthorizedRequest(filterInput);
        ResponseEntity<QuestionFilterResponseDto> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_QUESTIONS_FILTER,
                HttpMethod.POST,
                filterEntity,
                QuestionFilterResponseDto.class
        );
        QuestionFilterResponseDto questionData = response.getBody();

        model.addAttribute(
                ControllerSettings.ATTR_QUESTIONS,
                questionData != null ? questionData.getQuestions() : new ArrayList<>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_AUTHOR_ID,
                selAuthorId
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE,
                questionData != null ? questionData.getSelectedCourse() : null
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE_ID,
                courseId
        );
        model.addAttribute(
                ControllerSettings.ATTR_CURRENT_PAGE,
                questionData != null && questionData.getPage() != null ? questionData.getPage() : pagination.page()
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_PAGES,
                questionData != null && questionData.getTotalPages() != null ? questionData.getTotalPages() : 1
        );
        model.addAttribute(
                ControllerSettings.ATTR_TOTAL_ELEMENTS,
                questionData != null && questionData.getTotalElements() != null ? questionData.getTotalElements() : 0L
        );
        model.addAttribute(
                ControllerSettings.ATTR_PAGE_SIZE,
                questionData != null && questionData.getPageSize() != null ? questionData.getPageSize() : pagination.pageSize()
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    /**
     * Parses the author ID string to a Long, returning null if blank or non-numeric.
     */
    private Long parseAuthorId(String selAuthorId) {
        if (selAuthorId != null && !selAuthorId.isBlank()) {
            try {
                return Long.valueOf(selAuthorId);
            }
            catch (NumberFormatException ex) {
                log.warn(
                        "Ignoring non-numeric authorId filter value: {}",
                        selAuthorId
                );
            }
        }
        return null;
    }

    // ─── Author search ─────────────────────────────────────────────────────────

    @PostMapping("/search")
    public String searchAuthors(@RequestParam("search") String search, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = UriComponentsBuilder.fromUriString(apiBaseUrl + ControllerSettings.API_AUTHORS_SEARCH).queryParam(
                "name",
                search
        ).toUriString();
        AuthorDto[] authors = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                AuthorDto[].class
        ).getBody();
        model.addAttribute(
                ControllerSettings.ATTR_AUTHORS,
                authors != null ? authors : new AuthorDto[0]
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
        return ControllerSettings.VIEW_AUTHOR_LIST;
    }

    // ─── Author CRUD ───────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newAuthor(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info("Rendering new author form");
        model.addAttribute(
                ControllerSettings.ATTR_AUTHOR,
                new AuthorDto()
        );
        model.addAttribute(
                ControllerSettings.ATTR_EDIT_MODE,
                false
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
        return ControllerSettings.VIEW_AUTHOR_EDIT;
    }

    @PostMapping(
            {
                    "",
                    "/"
            }
    )
    public String createAuthor(@ModelAttribute AuthorDto authorDto, RedirectAttributes redirectAttributes, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        if (!validateAuthorDto(
                authorDto,
                false,
                redirectAttributes,
                model
        )) {
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }

        log.debug(
                "Sending AuthorDto to backend: {}",
                authorDto
        );
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);
        try {
            restTemplate.postForEntity(
                    apiBaseUrl + ControllerSettings.API_AUTHORS,
                    entity,
                    AuthorDto.class
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_CREATED_SUCCESS
            );
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        }
        catch (Exception ex) {
            log.error(
                    "Error creating author: {}",
                    ex.getMessage(),
                    ex
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_CREATE_FAILED_PREFIX + ex.getMessage()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    authorDto
            );
            model.addAttribute(
                    ControllerSettings.ATTR_EDIT_MODE,
                    false
            );
            model.addAttribute(
                    ControllerSettings.ATTR_LOGGED_IN_USER,
                    sessionService.getLoggedInUser()
            );
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
    }

    @PutMapping("/{id}")
    @PostMapping("/{id}")
    public String updateAuthor(@PathVariable Long id, @ModelAttribute AuthorDto authorDto, RedirectAttributes redirectAttributes, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        authorDto.setId(id);
        if (!validateAuthorDto(
                authorDto,
                true,
                redirectAttributes,
                model
        )) {
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }

        log.info(
                "Updating author id={}: {}",
                id,
                authorDto
        );
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);
        try {
            restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id,
                    HttpMethod.PUT,
                    entity,
                    AuthorDto.class
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_UPDATED_SUCCESS
            );
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        }
        catch (Exception ex) {
            log.error(
                    "Error updating author: {}",
                    ex.getMessage(),
                    ex
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_UPDATE_FAILED_PREFIX + ex.getMessage()
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    authorDto
            );
            model.addAttribute(
                    ControllerSettings.ATTR_EDIT_MODE,
                    true
            );
            model.addAttribute(
                    ControllerSettings.ATTR_LOGGED_IN_USER,
                    sessionService.getLoggedInUser()
            );
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
    }

    /**
     * Validates author DTO fields and populates model/redirectAttributes on failure.
     * Returns true if valid, false otherwise.
     */
    private boolean validateAuthorDto(AuthorDto authorDto, boolean editMode, RedirectAttributes redirectAttributes, Model model) {
        if (authorDto.getName() == null || authorDto.getName().isBlank()) {
            log.error(
                    "Author name is missing or blank. AuthorDto: {}",
                    authorDto
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_NAME_REQUIRED
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    authorDto
            );
            model.addAttribute(
                    ControllerSettings.ATTR_EDIT_MODE,
                    editMode
            );
            model.addAttribute(
                    ControllerSettings.ATTR_LOGGED_IN_USER,
                    sessionService.getLoggedInUser()
            );
            return false;
        }
        if (authorDto.getInitials() == null || authorDto.getInitials().isBlank()) {
            log.error(
                    "Author initials are missing or blank. AuthorDto: {}",
                    authorDto
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_INITIALS_REQUIRED
            );
            model.addAttribute(
                    ControllerSettings.ATTR_AUTHOR,
                    authorDto
            );
            model.addAttribute(
                    ControllerSettings.ATTR_EDIT_MODE,
                    editMode
            );
            model.addAttribute(
                    ControllerSettings.ATTR_LOGGED_IN_USER,
                    sessionService.getLoggedInUser()
            );
            return false;
        }
        return true;
    }

    @DeleteMapping("/{id}")
    public String deleteAuthor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info(
                "Deleting author with id: {}",
                id
        );
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
        redirectAttributes.addFlashAttribute(
                ControllerSettings.ATTR_MESSAGE,
                ControllerSettings.MSG_AUTHOR_DELETED_SUCCESS
        );
        return ControllerSettings.VIEW_REDIRECT_AUTHORS;
    }

    @GetMapping("/delete/{id}")
    @ResponseBody
    public String deleteAuthorAjax(@PathVariable Long id) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return ControllerSettings.MSG_NOT_AUTHENTICATED;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
        return ControllerSettings.MSG_AUTHOR_DELETED_AJAX;
    }

    // ─── Author edit form ──────────────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String editAuthorForm(
            @PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page, @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model, RedirectAttributes redirectAttributes
    ) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        PaginationParams pagination = PaginationSupport.normalize(
                page,
                pageSize
        );
        try {
            prepareAuthorData(
                    id,
                    model
            );
            model.addAttribute(
                    ControllerSettings.ATTR_BACK_TO_AUTHORS_URL,
                    buildAuthorsBackUrl(
                            courseId,
                            authorId,
                            questionBankId,
                            pagination.page(),
                            pagination.pageSize()
                    )
            );
            model.addAttribute(
                    ControllerSettings.ATTR_EDIT_MODE,
                    true
            );
            model.addAttribute(
                    ControllerSettings.ATTR_LOGGED_IN_USER,
                    sessionService.getLoggedInUser()
            );
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
        catch (HttpClientErrorException.NotFound ex) {
            log.error(
                    "Author with id {} not found",
                    id
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_AUTHOR_NOT_FOUND + id
            );
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        }
        catch (HttpClientErrorException.Forbidden ex) {
            log.error(
                    "403 Forbidden when accessing author {}: Token expired or insufficient permissions",
                    id
            );
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        catch (Exception ex) {
            log.error(
                    "Error loading author for edit with id {}: {}",
                    id,
                    ex.getMessage(),
                    ex
            );
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_ERROR_LOADING_AUTHOR_PREFIX + ex.getMessage()
            );
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        }
    }

    /**
     * Loads author by ID from the API and adds it to the model.
     * Throws {@link IllegalStateException} if the API returns null.
     */
    private void prepareAuthorData(Long id, Model model) {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        AuthorDto author = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id,
                HttpMethod.GET,
                entity,
                AuthorDto.class
        ).getBody();
        if (author == null) {
            log.warn(
                    "Author API returned null for id: {}",
                    id
            );
            throw new IllegalStateException("Author not found with id: " + id);
        }
        model.addAttribute(
                ControllerSettings.ATTR_AUTHOR,
                author
        );
    }

    // ─── Author details ────────────────────────────────────────────────────────

    @GetMapping("/{id}/details")
    public String showAuthorDetails(
            @PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page, @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model
    ) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn(
                    "Invalid session when accessing author details for id: {}",
                    id
            );
            return redirect;
        }

        PaginationParams pagination = PaginationSupport.normalize(
                page,
                pageSize
        );
        model.addAttribute(
                ControllerSettings.ATTR_BACK_TO_AUTHORS_URL,
                buildAuthorsBackUrl(
                        courseId,
                        authorId,
                        questionBankId,
                        pagination.page(),
                        pagination.pageSize()
                )
        );
        model.addAttribute(
                ControllerSettings.ATTR_EDIT_AUTHOR_URL,
                buildAuthorEditUrl(
                        id,
                        courseId,
                        authorId,
                        questionBankId,
                        pagination.page(),
                        pagination.pageSize()
                )
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_COURSE_ID,
                courseId
        );
        model.addAttribute(
                ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID,
                questionBankId
        );
        model.addAttribute(
                ControllerSettings.ATTR_CURRENT_PAGE,
                pagination.page()
        );
        model.addAttribute(
                ControllerSettings.ATTR_PAGE_SIZE,
                pagination.pageSize()
        );

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String url = apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id + ControllerSettings.API_AUTHORS_DETAILS_SUFFIX;
            log.info(
                    "Fetching author details from: {}",
                    url
            );
            AuthorDetailsDto details = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AuthorDetailsDto.class
            ).getBody();

            if (details == null || details.getAuthor() == null) {
                log.warn(
                        "No author details found for id: {}",
                        id
                );
                populateAuthorDetailsModelFallback(
                        model,
                        ControllerSettings.MSG_AUTHOR_DETAILS_NOT_FOUND
                );
                return ControllerSettings.VIEW_AUTHOR_DETAILS;
            }

            log.info(
                    "Successfully loaded details for author id: {}",
                    id
            );
            populateAuthorDetailsModelFromDto(
                    model,
                    details
            );
            return ControllerSettings.VIEW_AUTHOR_DETAILS;

        }
        catch (HttpClientErrorException.Forbidden ex) {
            log.error(
                    "403 Forbidden for author details id {}: Token expired or insufficient permissions",
                    id
            );
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        catch (HttpClientErrorException.NotFound ex) {
            log.warn(
                    "Author with id {} not found",
                    id
            );
            populateAuthorDetailsModelFallback(
                    model,
                    ControllerSettings.MSG_AUTHOR_NOT_FOUND_PLAIN
            );
            return ControllerSettings.VIEW_AUTHOR_DETAILS;
        }
        catch (Exception ex) {
            log.error(
                    "Error fetching author details for id {}: {}",
                    id,
                    ex.getMessage(),
                    ex
            );
            populateAuthorDetailsModelFallback(
                    model,
                    ControllerSettings.MSG_COULD_NOT_LOAD_AUTHOR_DETAILS
            );
            return ControllerSettings.VIEW_AUTHOR_DETAILS;
        }
    }

    /**
     * Populates model with author details data from details DTO.
     * Similar to populateQuestionListModelFromDto in ThyQuestionController.
     */
    private void populateAuthorDetailsModelFromDto(Model model, AuthorDetailsDto details) {
        model.addAttribute(
                ControllerSettings.ATTR_AUTHOR,
                details.getAuthor()
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTION_BANKS,
                details.getQuestionBanks()
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTIONS_BY_QUESTION_BANK,
                details.getQuestionsByQuestionBank()
        );
        model.addAttribute(
                ControllerSettings.ATTR_ERRORS_BY_QUESTION_BANK,
                details.getErrorsByQuestionBank()
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
    }

    /**
     * Populates model with fallback/error state for author details view.
     */
    private void populateAuthorDetailsModelFallback(Model model, String errorMessage) {
        model.addAttribute(
                ControllerSettings.ATTR_AUTHOR,
                new AuthorDto()
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTION_BANKS,
                new QuestionBankDto[0]
        );
        model.addAttribute(
                ControllerSettings.ATTR_QUESTIONS_BY_QUESTION_BANK,
                new HashMap<Long, List<QuestionDto>>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_ERRORS_BY_QUESTION_BANK,
                new HashMap<Long, List<QuestionErrorDto>>()
        );
        model.addAttribute(
                ControllerSettings.ATTR_LOGGED_IN_USER,
                sessionService.getLoggedInUser()
        );
        model.addAttribute(
                ControllerSettings.ATTR_ERROR_MESSAGE,
                errorMessage
        );
    }

    // ─── URL builders ──────────────────────────────────────────────────────────

    /**
     * Builds the back URL for the author list with optional filter parameters.
     */
    private String buildAuthorsBackUrl(Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize) {
        StringBuilder url = new StringBuilder("/authors");
        String sep = "?";
        if (courseId != null) {
            url.append(sep).append(ControllerSettings.ATTR_COURSE_ID).append("=").append(courseId);
            sep = "&";
        }
        if (authorId != null && authorId > 0) {
            url.append(sep).append(ControllerSettings.ATTR_AUTHOR_ID).append("=").append(authorId);
            sep = "&";
        }
        if (questionBankId != null && questionBankId > 0) {
            url.append(sep).append(ControllerSettings.ATTR_QUESTION_BANK_ID).append("=").append(questionBankId);
            sep = "&";
        }
        if (page != null && page > 0) {
            url.append(sep).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(page);
            sep = "&";
        }
        if (pageSize != null && pageSize > 0) {
            url.append(sep).append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pageSize);
        }

        return url.toString();
    }

    /**
     * Builds the edit URL for a given author, preserving the current filter context as query parameters.
     */
    private String buildAuthorEditUrl(Long id, Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize) {
        String backUrl = buildAuthorsBackUrl(
                courseId,
                authorId,
                questionBankId,
                page,
                pageSize
        );
        if (!backUrl.contains("?")) {
            return "/authors/edit/" + id;
        }
        return "/authors/edit/" + id + "?" + backUrl.substring(backUrl.indexOf('?') + 1);
    }
}
