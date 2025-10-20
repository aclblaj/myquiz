package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizErrorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.MyUtil;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.services.QuizAuthorService;
import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquiz.util.TemplateType;
import com.unitbv.myquizapi.dto.AuthorDto;
import com.unitbv.myquizapi.dto.AuthorErrorDto;
import com.unitbv.myquizapi.dto.QuestionDto;
import com.unitbv.myquizapi.dto.QuizDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthorServiceImpl implements AuthorService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorServiceImpl.class);
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuizRepository quizRepository;
    private final QuizErrorRepository quizErrorRepository;
    Logger log = LoggerFactory.getLogger(AuthorServiceImpl.class.getName());
    AuthorRepository authorRepository;
    QuestionRepository questionRepository;
    QuestionService questionService;
    String authorName;
    private ArrayList<Author> authors;
    AuthorErrorService authorErrorService;
    QuizAuthorService quizAuthorService;
    QuizService quizService;

    @Lazy
    @Autowired
    public AuthorServiceImpl(
            AuthorRepository authorRepository,
            QuestionRepository questionRepository,
            QuestionService questionService,
            QuizAuthorRepository quizAuthorRepository,
            QuizRepository quizRepository,
            AuthorErrorService authorErrorService,
            QuizAuthorService quizAuthorService,
            QuizService quizService, QuizErrorRepository quizErrorRepository) {
        this.authorRepository = authorRepository;
        this.questionRepository = questionRepository;
        this.questionService = questionService;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
        this.authorErrorService = authorErrorService;
        this.quizAuthorService = quizAuthorService;
        this.quizService = quizService;
        this.quizErrorRepository = quizErrorRepository;
    }

    private static boolean testIfMultichoice(Question q) {
        return q.getType().equals(QuestionType.MULTICHOICE);
    }

    public String extractAuthorNameFromPath(String filePath) {
        authorName = MyUtil.USER_NAME_NOT_DETECTED;

        Path path = Paths.get(filePath);
        if (path.toFile().exists()) {
            String lastDirectory = path.getParent().getFileName().toString();
            int endIndex = lastDirectory.indexOf("_");
            if (endIndex != -1) {
                authorName = lastDirectory.substring(0, endIndex);
            } else {
                log.error("Directory name '{}' not in the correct format (e.g.: 'John Doe_123'), use default '{}'"
                        , lastDirectory, authorName);
            }

        } else {
            log.error("Directory not found: {}", filePath);
        }
        return authorName;

    }

    public String extractInitials(String authorName) {
        String initials = "";
        if (authorName.length() > 0) {
            String[] split = authorName.split(" ");
            for (String s : split) {
                initials += s.charAt(0);
            }
        }
        return initials;
    }

    public Author saveAuthor(Author author) {
        logger.atInfo().log("Saving author: {}", author.getName());
        Author authorDb = authorRepository.findByName(author.getName()).orElse(null);
        if (authorDb != null) {
            log.atInfo().addArgument(author.getName())
               .log("Author '{}' already exists in the database");
            return authorDb;
        }
        return authorRepository.save(author);
    }

    public List<Author> getAllAuthors() {
        try {
            return authorRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting authors: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteAll() {
        authorRepository.deleteAll();
    }

    public AuthorDto getAuthorDTO(Author author, String courseName) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(author.getId());
        authorDto.setName(author.getName());
        authorDto.setInitials(author.getInitials());

        getQuizAuthors(author).forEach(quizAuthor -> {
            Quiz quiz = getQuizAuthorQuiz(quizAuthor);
            if (quiz != null && quiz.getCourse() != null && quiz.getCourse().equalsIgnoreCase(courseName)) {
                long noMC = getQuizAuthorQuestions(quizAuthor).stream()
                                                              .filter(q1 -> testIfMultichoice(q1) && isCourseNameEqualTo(courseName, q1))
                                                              .count();
                authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
                long noTF = getQuizAuthorQuestions(quizAuthor).stream()
                                                              .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                                                              .filter(q -> isCourseNameEqualTo(courseName, q))
                                                              .count();
                authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
                authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + getQuizAuthorQuizErrors(quizAuthor).size());
                authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
                authorDto.setQuizName(quiz.getName());
                if (authorDto.getQuizName() == null) {
                    int stop = 1;
                }
                if (quizAuthor.getTemplateType() != null) {
                    authorDto.setTemplateType(quizAuthor.getTemplateType().toString());
                } else {
                    authorDto.setTemplateType(TemplateType.Other.toString());
                }
                authorDto.setCourse(quiz.getCourse());
            }
        });
        return authorDto;
    }

    private boolean isCourseNameEqualTo(String courseName, Question q) {
        Quiz quiz = getQuizAuthorQuiz(q.getQuizAuthor());
        return (quiz != null && quiz.getCourse() != null && quiz.getCourse().equalsIgnoreCase(courseName)) || courseName.isEmpty();
    }

    @Override
    public Page<Author> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        return authorRepository.findAll(paging);
    }

    @Override
    public Page<Author> findPaginatedFiltered(String course, int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        return authorRepository.findAllByQuizAuthors_QuizCourse(course, paging);
    }

    @Override
    public void setAuthorsList(ArrayList<Author> authors) {
        this.authors = authors;
    }

    @Override
    public ArrayList<Author> getAuthorsList() {
        return authors;
    }

    @Override
    public void addAuthorToList(Author author) {
        if (authors == null) {
            authors = new ArrayList<>();
        }
        authors.add(author);
    }

    @Override
    public void deleteAuthorById(long id) {
        Author author = authorRepository.findById(id).orElse(null);
        if (author != null) {
            authorRepository.deleteById(id);
        } else {
            log.error("Author with id '{}' not found", id);
        }
    }

    @Override
    public boolean authorNameExists(String name) {
        boolean result = false;
        Author author = authorRepository.findByName(name).orElse(null);
        if (author != null) {
            result = true;
        }
        return result;
    }

    @Override
    public Author getAuthorByName(String name) {
        Author author = authorRepository.findByNameContainingIgnoreCase(name).orElse(null);
        return author;
    }

    public AuthorDto getAuthorDtoWillAllQuizzes(String name) {
        Author author = authorRepository.findByName(name).orElse(null);
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findByAuthorId(author.getId());
//        author.setQuizAuthors();
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(author.getId());
        authorDto.setName(author.getName());
        authorDto.setInitials(author.getInitials());
        getQuizAuthors(author).forEach(quizAuthor -> {
                long noMC = getQuizAuthorQuestions(quizAuthor).stream()
                                                              .filter(q1 -> testIfMultichoice(q1))
                                                              .count();
                authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
                long noTF = getQuizAuthorQuestions(quizAuthor).stream()
                                                              .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                                                              .count();
                authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
                authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + getQuizAuthorQuizErrors(quizAuthor).size());
                authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
                authorDto.setQuizName(getQuizAuthorQuiz(quizAuthor).getName());
                if (authorDto.getQuizName() == null) {
                    int stop = 1;
                }
                if (quizAuthor.getTemplateType() != null) {
                    authorDto.setTemplateType(quizAuthor.getTemplateType().toString());
                } else {
                    authorDto.setTemplateType(TemplateType.Other.toString());
                }
                authorDto.setCourse(getQuizAuthorQuiz(quizAuthor).getCourse());
        });
        return authorDto;
    }

    private  Quiz getQuizAuthorQuiz(QuizAuthor quizAuthor) {
        Set<QuizAuthor> quizAuthors = new HashSet<>();
        quizAuthors.add(quizAuthor);
        return quizRepository.findByQuizAuthors(quizAuthors);
        //quizAuthor.getQuiz();
    }

    private List<QuizError> getQuizAuthorQuizErrors(QuizAuthor quizAuthor) {
        return quizErrorRepository.findByQuizAuthorId(quizAuthor.getId());
    }

    private List<Question> getQuizAuthorQuestions(QuizAuthor quizAuthor) {
        return questionRepository.findByQuizAuthorId(quizAuthor.getId());
    }

    private List<QuizAuthor> getQuizAuthors(Author author) {
        return quizAuthorRepository.findByAuthorId(author.getId());
    }

    @Override
    public List<QuizAuthor> getQuizAuthorsForAuthorId(Long authorId) {
        return quizAuthorRepository.findWithQuestionsByAuthorId(authorId);
    }

    @Override
    public void deleteQuizAuthorsByIds(List<Long> idsQA) {
        quizAuthorRepository.deleteAllById(idsQA);
    }

    @Override
    public List<String> getCourseNames() {
        List<Quiz> quizzes = quizRepository.findAll();
        if (quizzes != null) {
            List<String> courses = new ArrayList<>();
            quizzes.forEach(quiz -> {
                if (!courses.contains(quiz.getCourse())) {
                    courses.add(quiz.getCourse());
                }
            });
            courses.sort(String::compareToIgnoreCase);
            return courses;
        }
        return List.of();
    }

    @Override
    public void deleteAuthorsWithoutQuiz() {
        List<Author> authors = authorRepository.findAll();
        authors.forEach(author -> {
            if (getQuizAuthors(author).size() == 0) {
                authorRepository.delete(author);
            }
        });
    }

    public ArrayList<String> getCoursesNamesAsStrings() {
        ArrayList<String> courses = new ArrayList<String>();
        courses.addAll(getCourseNames());
        return courses;
    }

    @Override
    public void prepareFilterAuthorsModel(Model model, String selectedCourse) {
        List<AuthorDto> authorDtos = new ArrayList<>();

        ArrayList<String> courses = getCoursesNamesAsStrings();
        model.addAttribute("courses", courses);

        if (selectedCourse == null) {
            selectedCourse = courses.size()>0 ? courses.get(0) : "";
        }

        int pageNo = 1;
        Page<Author> page =  findPaginatedFiltered(selectedCourse, pageNo, MyUtil.PAGE_SIZE, "name", "desc");
        String finalSelectedCourse = selectedCourse;
        page.stream().forEach(author -> authorDtos.add(getAuthorDTO(author, finalSelectedCourse)));
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("authors", authorDtos);
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("route", "authors/");
    }

    public void prepareAuthorModelData(Model model, String authorName) {
        Author author = getAuthorByName(authorName);
        AuthorDto authorDTO = getAuthorDtoWillAllQuizzes(author.getName());

        List<QuizDto> quizDtos = new ArrayList<>();
        quizAuthorService.getQuizAuthorsForAuthorName(authorName)
                         .forEach(quizAuthor -> {
                             Quiz quiz = getQuizAuthorQuiz(quizAuthor);
                             QuizDto quizDto = new QuizDto();
                             quizDto.setId(quiz.getId());
                             quizDto.setName(quiz.getName());
                             quizDto.setCourse(quiz.getCourse());
                             quizDto.setYear(quiz.getYear());
                             quizDto.setSourceFile(quizAuthor.getSource());
                             quizDto.setQuizAuthorId(quizAuthor.getId());
                             quizDtos.add(quizDto);
                         });

        quizDtos.sort((q1, q2) -> quizService.getCompareTo(q1, q2));

        quizDtos.forEach(quizDto -> {
            List<QuestionDto> questionDtos = new ArrayList<>();
            List<QuestionDto> questionDtosTF = new ArrayList<>();
            List<Question> quizQuestions = questionService.getQuizzQuestionsForAuthor(quizDto.getQuizAuthorId());
            quizQuestions.forEach(question -> {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    QuestionDto dto = new QuestionDto();
                    dto.setId(question.getId());
                    dto.setTitle(question.getTitle());
                    dto.setText(question.getText());
                    dto.setChapter(question.getChapter());
                    questionDtos.add(dto);
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    QuestionDto dto = new QuestionDto();
                    dto.setId(question.getId());
                    dto.setTitle(question.getTitle());
                    dto.setText(question.getText());
                    dto.setChapter(question.getChapter());
                    questionDtosTF.add(dto);
                }
            });
            List<AuthorErrorDto> authorErrorDtos = authorErrorService.getErrorsForQuizAuthor(quizDto.getQuizAuthorId()).stream()
                                                                     .map(error -> new AuthorErrorDto(
                                                                         error.getDescription(),
                                                                         error.getRowNumber(),
                                                                         error.getQuizAuthor() != null && error.getQuizAuthor().getAuthor() != null ?
                                                                             error.getQuizAuthor().getAuthor().getName() : "Unknown",
                                                                         error.getQuizAuthor() != null && error.getQuizAuthor().getAuthor() != null ?
                                                                             error.getQuizAuthor().getAuthor().getId() : null
                                                                     ))
                                                                     .toList();
            authorErrorDtos = authorErrorService.sortAuthorErrorsByRow(authorErrorDtos);

            quizDto.setQuestionDtosMC(questionDtos);
            quizDto.setQuestionDtosTF(questionDtosTF);
            quizDto.setAuthorErrorDtos(authorErrorDtos);
            // Also set the fields expected by the template
            quizDto.setQuestionsMC(questionDtos);
            quizDto.setQuestionsTF(questionDtosTF);
        });

        model.addAttribute("quizzes", quizDtos);
        // Set 'quiz' to the first quiz or null if none
        model.addAttribute("quiz", quizDtos.size() > 0 ? quizDtos.get(0) : null);
        // get all course authors
        List<Author> aList = getAuthorsByCourse(quizDtos.size() > 0 ? quizDtos.get(0).getCourse() : "");
        List<AuthorDto> aDtoList = new ArrayList<>();
        aList.forEach(a -> aDtoList.add(getAuthorDTO(a, quizDtos.size() > 0 ? quizDtos.get(0).getCourse() : "")));

        List<AuthorDto> authorsList = new ArrayList<>();
        if (aDtoList.size() > 0) {
            aDtoList.sort((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));
            authorsList.addAll(aDtoList);
        } else {
            // Set 'authors' to a list containing the current author
            if (authorDTO != null) {
                authorsList.add(authorDTO);
            }
        }
        model.addAttribute("authors", authorsList);
        model.addAttribute("author", authorDTO);
    }

    @Override
    public List<Author> getAuthorsByCourse(String course) {
        return quizAuthorRepository.findAllByQuiz_CourseContainsIgnoreCase(course)
                                   .stream()
                                   .map(QuizAuthor::getAuthor)
                                   .distinct()
                                   .toList();
    }

    @Override
    public Author getAuthorById(Long selectedAuthorObj) {
        return authorRepository.findById(selectedAuthorObj).orElse(null);
    }
}
