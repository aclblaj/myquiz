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
    public static final String REDIRECT_HOME = "redirect:/quiz";
    public static final String VIEW_LOGIN = "login";
    public static final String VIEW_REGISTER = "register";
    public static final String DEFAULT_REGISTRATION_ERROR = "Registration failed. Try again.";
    public static final String DEFAULT_INTERNAL_ERROR = "Internal server error. Try again.";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_TOKEN = "token";
    // Course
    public static final String ATTR_COURSES = "courses";
    public static final String ATTR_COURSE = "course";
    public static final String ATTR_MESSAGE = "message";
    public static final String VIEW_COURSE_LIST = "course-list";
    public static final String VIEW_COURSE_EDIT = "course-edit";
    public static final String VIEW_COURSE_DETAILS = "course-details";
    public static final String VIEW_REDIRECT_COURSES = "redirect:/courses";
    public static final String API_COURSES = "/courses/";
    // Question
    public static final String ATTR_QUESTIONS = "questions";
    public static final String ATTR_AUTHORS = "authors";
    public static final String ATTR_QUIZZES = "quizzes";
    public static final String ATTR_QUESTION = "question";
    public static final String ATTR_SUCCESS_MESSAGE = "successMessage";
    public static final String ATTR_ERROR_MESSAGE = "errorMessage";
    public static final String VIEW_QUESTION_LIST = "question-list";
    public static final String QUESTION_EDITOR_MULTICHOICE = "question-editor-mc";
    public static final String QUESTION_EDITOR_TRUEFALSE = "question-editor-tf";
    public static final String VIEW_REDIRECT_QUESTIONS = "redirect:/questions";
    public static final String API_QUESTIONS = "/questions";
    public static final String API_QUESTIONS_FILTER = "/questions/filter";
    public static final String API_QUIZZES = "/quizzes";
    public static final String API_AUTHORS = "/authors";
    public static final String API_AUTHORS_BY_NAME = "/authors/name/";
    public static final String DEFAULT_AUTHOR = "Max Mustermann";
    public static final String DEFAULT_COURSE = "Unknown Course";
    public static final String DEFAULT_QUIZ = "Default Quiz";
    // Quiz
    public static final String ATTR_QUIZ = "quiz";
    public static final String ATTR_QUIZ_FILTER = "quizFilter";
    public static final String ATTR_ERROR_MSG = "errorMsg";
    public static final String VIEW_QUIZ_LIST = "quiz-list";
    public static final String VIEW_QUIZ_DETAILS = "quiz-details";
    public static final String VIEW_QUIZ_EDITOR = "quiz-editor";
    public static final String VIEW_REDIRECT_QUIZ = "redirect:/quiz";
    public static final String VIEW_REDIRECT_AUTH_LOGIN = "redirect:/auth/login";
    public static final String API_QUIZZES_SLASH = "/quizzes/";
    // Author
    public static final String ATTR_SELECTED_COURSE = "selectedCourse";
    public static final String ATTR_SELECTED_AUTHOR_ID = "selectedAuthorId";
    public static final String ATTR_SELECTED_AUTHOR = "selectedAuthor";
    public static final String VIEW_AUTHOR_LIST = "author-list";
    public static final String VIEW_AUTHOR_EDIT = "author-edit";
    public static final String VIEW_AUTHOR_DETAILS = "author-details";
    public static final String VIEW_REDIRECT_AUTHORS = "redirect:/authors";
    public static final String VIEW_SUCCESS = "success";
    // Error
    public static final String VIEW_ERROR_LIST = "error-list";
    public static final String API_ERRORS = "/errors";
    public static final String API_ERRORS_FILTER = "/errors/filter";
    public static final String ATTR_ERRORS = "errors";

    public static final String ATTR_CURRENT_PAGE = "currentPage";
    public static final String ATTR_EDIT_MODE = "editMode";
    public static final String API_UPLOAD_EXCEL = "/upload/excel";
    public static final String API_UPLOAD_ARCHIVE = "/upload/archive";

    public static final String ATTR_QUESTIONS_BY_QUIZ = "questionsByQuiz";
    public static final String ATTR_ERRORS_BY_QUIZ = "errorsByQuiz";

    // Additional question/pagination attributes
    public static final String ATTR_AUTHOR_ID = "authorId";
    public static final String ATTR_QUIZ_ID = "quizId";
    public static final String ATTR_SELECTED_TYPE = "selectedType";
    public static final String ATTR_SELECTED_QUIZ_ID = "selectedQuizId";
    public static final String ATTR_AUTHOR_NAME = "authorName";

    public static final String UNKNOWN = "Unknown";

    private ControllerSettings() {}

}
