package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.dto.AuthorErrorDto;
import com.unitbv.myquiz.dto.QuestionDto;
import com.unitbv.myquiz.dto.QuizDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.util.TemplateType;
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
import java.util.List;

@Service
public class AuthorServiceImpl implements AuthorService{
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuizRepository quizRepository;
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
            QuestionService questionService,
            QuizAuthorRepository quizAuthorRepository,
            QuizRepository quizRepository,
            AuthorErrorService authorErrorService,
            QuizAuthorService quizAuthorService,
            QuizService quizService) {
        this.authorRepository = authorRepository;
        this.questionService = questionService;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
        this.authorErrorService = authorErrorService;
        this.quizAuthorService = quizAuthorService;
        this.quizService = quizService;
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
        AuthorDto authorDto = new AuthorDto(author);
        author.getQuizAuthors().forEach(quizAuthor -> {
            if (quizAuthor.getQuiz().getCourse().equalsIgnoreCase(courseName)) {
                long noMC = quizAuthor.getQuestions().stream()
                                      .filter(q1 -> testIfMultichoice(q1) && isCourseNameEqualTo(courseName, q1))
                                      .count();
                authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
                long noTF = quizAuthor.getQuestions().stream()
                                      .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                                      .filter(q -> isCourseNameEqualTo(courseName, q))
                                      .count();
                authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
                authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + quizAuthor.getQuizErrors().size());
                authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
                authorDto.setQuizName(quizAuthor.getQuiz().getName());
                if (authorDto.getQuizName() == null) {
                    int stop = 1;
                }
                if (quizAuthor.getTemplateType() != null) {
                    authorDto.setTemplateType(quizAuthor.getTemplateType());
                } else {
                    authorDto.setTemplateType(TemplateType.Other);
                }
                authorDto.setCourse(quizAuthor.getQuiz().getCourse());
            }
        });
        return authorDto;
    }

    private static boolean isCourseNameEqualTo(String courseName, Question q) {
        return q.getQuizAuthor().getQuiz().getCourse().equalsIgnoreCase(courseName) || courseName.isEmpty();
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
        Author author = authorRepository.findByName(name).orElse(null);
        return author;
    }

    public AuthorDto getAuthorDtoWillAllQuizzes(String name) {
        Author author = authorRepository.findByName(name).orElse(null);
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findByAuthorId(author.getId());
        author.setQuizAuthors();
        AuthorDto authorDto = new AuthorDto(author);
        author.getQuizAuthors().forEach(quizAuthor -> {
                long noMC = quizAuthor.getQuestions().stream()
                                      .filter(q1 -> testIfMultichoice(q1))
                                      .count();
                authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
                long noTF = quizAuthor.getQuestions().stream()
                                      .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                                      .count();
                authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
                authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + quizAuthor.getQuizErrors().size());
                authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
                authorDto.setQuizName(quizAuthor.getQuiz().getName());
                if (authorDto.getQuizName() == null) {
                    int stop = 1;
                }
                if (quizAuthor.getTemplateType() != null) {
                    authorDto.setTemplateType(quizAuthor.getTemplateType());
                } else {
                    authorDto.setTemplateType(TemplateType.Other);
                }
                authorDto.setCourse(quizAuthor.getQuiz().getCourse());
        });
        return authorDto;
    }

    @Override
    public List<QuizAuthor> getQuizAuthorsForAuthorId(Long authorId) {
        return quizAuthorRepository.findByAuthorId(authorId);
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
            if (author.getQuizAuthors().size() == 0) {
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
                             QuizDto quizDto = new QuizDto(quizAuthor.getQuiz());
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
                    questionDtos.add(new QuestionDto(question));
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    questionDtosTF.add(new QuestionDto(question));
                }
            });
            List<AuthorErrorDto> authorErrorDtos = authorErrorService.getErrorsForQuizAuthor(quizDto.getQuizAuthorId()).stream()
                                                                     .map(AuthorErrorDto::new)
                                                                     .toList();
            authorErrorDtos = authorErrorService.sortAuthorErrorsByRow(authorErrorDtos);

            quizDto.setQuestionDtosMC(questionDtos);
            quizDto.setQuestionDtosTF(questionDtosTF);
            quizDto.setAuthorErrorDtos(authorErrorDtos);
        });

        model.addAttribute("quizzes", quizDtos);
        model.addAttribute("author", authorDTO);
    }
}
