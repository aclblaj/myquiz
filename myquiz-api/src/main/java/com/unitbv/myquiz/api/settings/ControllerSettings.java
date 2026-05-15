package com.unitbv.myquiz.api.settings;

public class ControllerSettings {

    public static final int PAGE_SIZE = 10;
    public static final String DEFAULT_PAGE = "1";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String ATTR_PAGE_SIZE = "pageSize";
    public static final String ATTR_PAGE_NUMBER= "page";
    public static final String ATTR_TOTAL_PAGES = "totalPages";
    public static final String ATTR_TOTAL_ELEMENTS = "totalElements";

    public static final String ATTR_AUTHOR = "author";
    public static final String HEADER_AUTHORIZATION = "Authorization" ;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ATTR_AUTHOR_LIST = "authorList";

    public static final String ATTR_LOGGED_IN_USER = "loggedInUser";
    public static final String ATTR_JWT_TOKEN = "jwtToken";
    public static final String ATTR_LOGIN_ERROR = "loginError";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_REGISTER_ERROR = "registerError";
    public static final String ATTR_REGISTER_ERROR_MSG = "registerErrorMsg";
    public static final String REDIRECT_HOME = "redirect:/question-banks";
    public static final String VIEW_LOGIN = "login";
    public static final String VIEW_REGISTER = "register";
    public static final String DEFAULT_REGISTRATION_ERROR = "Registration failed. Try again.";
    public static final String DEFAULT_INTERNAL_ERROR = "Internal server error. Try again.";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_TOKEN = "token";
    // Course
    public static final String ATTR_COURSES = "courses";
    public static final String ATTR_COURSE = "course";
    public static final String ATTR_COURSE_ID = "courseId";
    public static final String ATTR_MESSAGE = "message";
    public static final String VIEW_COURSE_LIST = "course-list";
    public static final String VIEW_COURSE_EDIT = "course-edit";
    public static final String VIEW_COURSE_DETAILS = "course-details";
    public static final String VIEW_REDIRECT_COURSES = "redirect:/courses";
    public static final String API_COURSES = "/courses/";
    // Question
    public static final String ATTR_QUESTIONS = "questions";
    public static final String ATTR_AUTHORS = "authors";
    public static final String ATTR_QUESTION_BANKS = "questionBanks";
    public static final String ATTR_QUESTION = "question";
    public static final String ATTR_SUCCESS_MESSAGE = "successMessage";
    public static final String ATTR_ERROR_MESSAGE = "errorMessage";
    public static final String VIEW_QUESTION_LIST = "question-list";
    public static final String VIEW_QUESTION_DUPLICATES = "question-duplicates";
    public static final String QUESTION_EDITOR_MULTICHOICE = "question-editor-mc";
    public static final String QUESTION_EDITOR_TRUEFALSE = "question-editor-tf";
    public static final String VIEW_REDIRECT_QUESTIONS = "redirect:/questions";
    public static final String API_QUESTIONS = "/questions";
    public static final String API_QUESTIONS_FILTER = "/questions/filter";
    public static final String API_QUESTION_BANKS = "/question-banks";
    public static final String API_QUESTION_BANKS_SLASH = "/question-banks/";
    public static final String API_QUESTION_BANKS_FILTER = "/question-banks/filter";
    public static final String API_QUESTION_BANKS_GET_BY_ID = "/question-banks/{questionBankId}";
    public static final String API_QUESTION_BANKS_GET_BY_ID_AND_AUTHOR_ID = "/author/{authorId}/question-banks/{questionBankId}";
    public static final String API_QUESTION_BANKS_DUPLICATES_REMOVE_BY_ID = "/{id:\\d+}/duplicates/remove";
    public static final String API_AUTHORS = "/authors";
    public static final String API_AUTHORS_BY_NAME = "/authors/name/";
    public static final String DEFAULT_AUTHOR = "Max Mustermann";
    public static final String DEFAULT_COURSE = "Unknown Course";
    public static final String DEFAULT_QUESTION_BANK = "Default Question Bank";
    // Question bank
    public static final String ATTR_SELECTED_QUESTION_BANK = "selectedQuestionBank";
    public static final String ATTR_QUESTION_BANK = "questionBank";
    public static final String ATTR_QUESTION_BANK_FILTER = "questionBankFilter";
    public static final String ATTR_ERROR_MSG = "errorMsg";
    public static final String VIEW_QUESTION_BANK_LIST = "question-bank-list";
    public static final String VIEW_QUESTION_BANK_DETAILS = "question-bank-details";
    public static final String QUESTION_BANK_EDITOR = "question-bank-editor";
    public static final String VIEW_REDIRECT_QUESTION_BANK = "redirect:/question-banks";
    public static final String VIEW_REDIRECT_QUESTION_BANKS = "redirect:/question-banks";
    public static final String VIEW_REDIRECT_AUTH_LOGIN = "redirect:/auth/login";
    // Author
    public static final String ATTR_SELECTED_COURSE = "selectedCourse";
    public static final String ATTR_SELECTED_COURSE_ID = "selectedCourseId";
    public static final String ATTR_SELECTED_AUTHOR_ID = "selectedAuthorId";
    public static final String ATTR_SELECTED_AUTHOR = "selectedAuthor";
    public static final String VIEW_AUTHOR_LIST = "author-list";
    public static final String VIEW_AUTHOR_EDIT = "author-edit";
    public static final String VIEW_AUTHOR_DETAILS = "author-details";
    public static final String VIEW_REDIRECT_AUTHORS = "redirect:/authors";
    public static final String VIEW_SUCCESS = "success";

    public static final String ATTR_CURRENT_PAGE = "currentPage";
    public static final String ATTR_EDIT_MODE = "editMode";
    public static final String API_UPLOAD_EXCEL = "/upload/excel";
    public static final String API_UPLOAD_ARCHIVE = "/upload/archive";
    public static final String API_UPLOAD_ARCHIVE_FOLDER = "/upload/archive-folder";
    public static final String API_UPLOAD_XML = "/upload/xml";

    public static final String ATTR_QUESTIONS_BY_QUESTION_BANK = "questionsByQuestionBank";
    public static final String ATTR_ERRORS_BY_QUESTION_BANK = "errorsByQuestionBank";

    // Additional question/pagination attributes
    public static final String ATTR_AUTHOR_ID = "authorId";
    public static final String ATTR_QUESTION_BANK_ID = "questionBankId";
    public static final String ATTR_SELECTED_TYPE = "selectedType";
    public static final String ATTR_SELECTED_QUESTION_BANK_ID = "selectedQuestionBankId";
    public static final String ATTR_AUTHOR_NAME = "authorName";

    // Question correction
    public static final String ATTR_CORRECTION_DTO = "correctionDto";
    public static final String VIEW_QUESTION_CORRECTION = "question-correction";
    public static final String RESPONSE_KEY_ALTERNATIVES = "alternatives";
    public static final String RESPONSE_KEY_EXPLANATION = "explanation";
    public static final String RESPONSE_KEY_STATUS = "status";
    public static final String RESPONSE_VALUE_SUCCESS = "success";

    // Home/Success page
    public static final String VIEW_HELP = "help";
    public static final String ATTR_MESSAGE_TYPE = "messageType";
    public static final String MESSAGE_TYPE_ERROR = "error";
    public static final String MESSAGE_TYPE_SUCCESS = "success";

    // PDF Check
    public static final String VIEW_CHECK_PDF = "check-pdf";
    public static final String RESPONSE_KEY_MESSAGE = "message";
    public static final String RESPONSE_KEY_ONLINE = "online";

    // Upload
    public static final String VIEW_UPLOAD_EXCEL = "upload-excel";
    public static final String VIEW_UPLOAD_ARCHIVE = "upload-archive";
    public static final String VIEW_UPLOAD_ARCHIVE_SINGLE = "upload-archive";
    public static final String VIEW_UPLOAD_ARCHIVE_FOLDER = "upload-archive-folder";
    public static final String VIEW_UPLOAD_XML = "upload-xml";
    public static final String ATTR_TEMPLATES = "templates";
    public static final String ATTR_MESSAGE_BOX_LINES = "messageBoxLines";

    public static final String UNKNOWN = "Unknown";

    // Errors
    public static final String VIEW_ERROR_LIST = "error-list";
    public static final String VIEW_REDIRECT_ERRORS = "redirect:/errors";
    public static final String API_ERRORS = "/errors";
    public static final String API_ERRORS_FILTER = "/errors/filter";
    public static final String ATTR_QUESTION_ERRORS = "questionErrors";
    public static final String ATTR_QUESTION_ERRORS_BY_AUTHOR = "questionErrorsByAuthor";
    public static final String ATTR_SELECTED_QUESTION_BANK_ID_ERRORS = "selectedQuestionBankIdErrors";
    public static final String ATTR_SELECTED_YEAR = "selectedYear";
    public static final String ATTR_YEARS = "years";
    public static final String ATTR_AUTHOR_NAMES = "authorNames";
    public static final String ATTR_BACK_TO_ERRORS_URL = "backToErrorsUrl";
    public static final String ATTR_BACK_URL = "backUrl";
    public static final String ATTR_BACK_TO_AUTHORS_URL = "backToAuthorsUrl";
    public static final String ATTR_EDIT_AUTHOR_URL = "editAuthorUrl";
    public static final String ATTR_BACK_TO_QUESTION_BANK_URL = "backToQuestionBankUrl";
    public static final String ATTR_BACK_TO_QUESTIONS_URL = "backToQuestionsUrl";
    public static final String ATTR_QUESTION_EDIT_URL = "questionEditUrl";
    public static final String ATTR_QUESTION_DUPLICATES_URL = "questionDuplicatesUrl";
    public static final String ATTR_QUESTION_CORRECTION_URL = "questionCorrectionUrl";
    public static final String ATTR_QUESTION_VIEW_URL = "questionViewUrl";
    public static final String ATTR_JWT_TOKEN_PRESENT = "jwtTokenPresent";
    public static final String ATTR_QUESTION_BANK_EXTENDED = "questionBankExtended";
    public static final String ATTR_AUTHOR_SECTIONS = "authorSections";
    public static final String ATTR_STATISTICS = "statistics";
    public static final String ATTR_PERMISSIONS = "permissions";
    public static final String ATTR_ROLES = "roles";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_DUPLICATES = "duplicates";
    public static final String ATTR_REGISTRATION_SUCCESS = "registrationSuccess";
    public static final String ATTR_REGISTRATION_MESSAGE = "registrationMessage";
    public static final String ERROR_STATUS_OPEN = "OPEN";
    public static final String ERROR_STATUS_RESOLVED = "RESOLVED";

    private ControllerSettings() {}

}
