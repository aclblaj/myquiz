package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDataDto;
import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorErrorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.Quiz;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuizErrorRepository;
import com.unitbv.myquiz.app.repositories.QuizRepository;
import com.unitbv.myquiz.app.specifications.AuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuizErrorSpecification;
import com.unitbv.myquiz.app.specifications.QuizSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AuthorService {
    private static final Logger log = LoggerFactory.getLogger(AuthorService.class.getName());

    // All dependencies are now final for thread safety
    private final AuthorRepository authorRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuizRepository quizRepository;
    private final QuizErrorService quizErrorService;
    private final QuizAuthorService quizAuthorService;
    private final QuizService quizService;
    private final QuizErrorRepository quizErrorRepository;
    private final QuestionMapper questionMapper;

    @Lazy
    @Autowired
    public AuthorService(AuthorRepository authorRepository, QuestionRepository questionRepository,
                         QuestionService questionService, QuizAuthorRepository quizAuthorRepository,
                         QuizRepository quizRepository, QuizErrorService quizErrorService,
                         QuizAuthorService quizAuthorService, QuizService quizService,
                         QuizErrorRepository quizErrorRepository, QuestionMapper questionMapper) {
        this.authorRepository = authorRepository;
        this.questionRepository = questionRepository;
        this.questionService = questionService;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
        this.quizErrorService = quizErrorService;
        this.quizAuthorService = quizAuthorService;
        this.quizService = quizService;
        this.quizErrorRepository = quizErrorRepository;
        this.questionMapper = questionMapper;
    }

    private static boolean testIfMultichoice(Question q) {
        return q.getType() != null && q.getType().equals(QuestionType.MULTICHOICE);
    }

    public String extractAuthorNameFromPath(String filePath) {
        String authorName = MyUtil.USER_NAME_NOT_DETECTED;

        Path path = Paths.get(filePath);
        if (path.toFile().exists()) {
            String lastDirectory = path.getParent().getFileName().toString();
            int endIndex = lastDirectory.indexOf("_");
            if (endIndex != -1) {
                authorName = lastDirectory.substring(0, endIndex);
            } else {
                log.error("Directory name '{}' not in the correct format (e.g.: 'John Doe_123'), use default '{}'", lastDirectory, authorName);
            }

        } else {
            log.error("Directory not found: {}", filePath);
        }
        return authorName;

    }

    public String extractInitials(String authorName) {
        StringBuilder initials = new StringBuilder();
        if (!authorName.isEmpty()) {
            String[] split = authorName.split(" ");
            for (String s : split) {
                initials.append(s.charAt(0));
            }
        }
        return initials.toString();
    }


    public AuthorDto saveAuthorDto(AuthorDto authorDto) {
        AuthorDto dto;
        if (authorDto.getId() == null) {
            Specification<Author> spec = AuthorSpecification.byName(authorDto.getName());
            Author existingAuthor = authorRepository.findOne(spec).orElse(null);
            if (existingAuthor != null) {
                log.info("Author with name '{}' already exists with id '{}'", authorDto.getName(), existingAuthor.getId());
                dto = new AuthorDto(existingAuthor.getId(), existingAuthor.getName(), existingAuthor.getInitials());
            } else {
                log.info("No existing author found with name '{}', proceeding to create new author", authorDto.getName());
                Author author = new Author();
                author.setName(authorDto.getName());
                author.setInitials(authorDto.getInitials());
                authorRepository.save(author);
                dto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
            }
        } else {
            Author author = authorRepository.findById(authorDto.getId()).orElse(null);
            if (author != null) {
                author.setName(authorDto.getName());
                author.setInitials(authorDto.getInitials());
                authorRepository.save(author);
                dto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
            } else {
                // ID provided but not found; create new
                Author newAuthor = new Author();
                newAuthor.setName(authorDto.getName());
                newAuthor.setInitials(authorDto.getInitials());
                authorRepository.save(newAuthor);
                dto = new AuthorDto(newAuthor.getId(), newAuthor.getName(), newAuthor.getInitials());
            }
        }
        return dto;
    }


    public List<AuthorDto> getAllAuthors() {
        try {
            return authorRepository.findAll().stream().map(a -> {
                AuthorDto dto = new AuthorDto(a.getId(), a.getName(), a.getInitials());
                // Compute question counts for each author
                long mcCount = 0L;
                long tfCount = 0L;
                long totalCount = 0L;
                List<QuizAuthor> quizAuthors = getQuizAuthors(a);
                for (QuizAuthor qa : quizAuthors) {
                    mcCount += getQuizAuthorQuestions(qa).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.MULTICHOICE)).count();
                    tfCount += getQuizAuthorQuestions(qa).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE)).count();
                }
                totalCount = mcCount + tfCount;
                dto.setNumberOfMultipleChoiceQuestions(mcCount);
                dto.setNumberOfTrueFalseQuestions(tfCount);
                dto.setNumberOfQuestions(totalCount);
                return dto;
            }).toList();
        } catch (Exception e) {
            log.error("Error getting authors: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all authors with basic information only (id, name, initials).
     * This is a lightweight method suitable for dropdown lists and filters.
     * Does NOT compute question counts - use getAllAuthors() if you need those.
     *
     * @return List of AuthorDto with basic info only
     */
    @Cacheable("allAuthorsBasic")
    public List<AuthorInfo> getAllAuthorsBasic() {
        try {
            return authorRepository.findAll().stream()
                    .map(a -> new AuthorInfo(a.getId(), a.getName()))
                    .toList();
        } catch (Exception e) {
            log.error("Error getting authors: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    public boolean existsById(Long id) {
        return authorRepository.existsById(id);
    }


    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }


    public void deleteAll() {
        authorRepository.deleteAll();
    }


    public AuthorDto getAuthorDTO(Long authorId, String courseName) {
        if (authorId == null) return null;
        Author author = authorRepository.findById(authorId).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        getQuizAuthors(author).forEach(quizAuthor -> {
            Quiz quiz = getQuizAuthorQuiz(quizAuthor);
            if (quiz != null && quiz.getCourse() != null && quiz.getCourse().equalsIgnoreCase(courseName)) {
                long noMC = getQuizAuthorQuestions(quizAuthor)
                        .stream()
                        .filter(q1 -> testIfMultichoice(q1) && isCourseNameEqualTo(courseName, q1))
                        .count();
                authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);

                long noTF = getQuizAuthorQuestions(quizAuthor)
                        .stream()
                        .filter(q -> testIfTruefalse(q) && isCourseNameEqualTo(courseName, q))
                        .count();
                authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);

                log.atInfo().log("Author '{}', Quiz '{}', Course '{}': MCQ count = {}, TF count = {}", author.getName(), quiz.getName(), quiz.getCourse(), noMC, noTF);

                authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + getQuizAuthorQuizErrors(quizAuthor).size());
                authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
                authorDto.setQuizName(quiz.getName());
                authorDto.setTemplateType(quizAuthor.getTemplateType() != null ? quizAuthor.getTemplateType().toString() : TemplateType.Other.toString());
                authorDto.setCourse(quiz.getCourse());
            }
        });
        return authorDto;
    }

    private boolean testIfTruefalse(Question q) {
        return q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE);
    }

    private boolean isCourseNameEqualTo(String courseName, Question q) {
        Quiz quiz = getQuizAuthorQuiz(q.getQuizAuthor());
        return (quiz != null && quiz.getCourse() != null && quiz.getCourse().equalsIgnoreCase(courseName)) || (courseName == null || courseName.isEmpty());
    }


    public Page<AuthorDto> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        Page<Author> page = authorRepository.findAll(paging);
        List<AuthorDto> content = page.getContent().stream().map(a -> new AuthorDto(a.getId(), a.getName(), a.getInitials())).toList();
        return new PageImpl<>(content, paging, page.getTotalElements());
    }


    public Page<AuthorDto> findPaginatedFiltered(String course, Long authorId,
                                                 int pageNo, int pageSize, String sortField, String sortDirection) {
        log.atInfo().log(
                "Finding paginated filtered authors - course: '{}', authorId: '{}', " +
                        "pageNo: {}, pageSize: {}, sortField: {}, sortDirection: {}",
                course, authorId, pageNo, pageSize, sortField, sortDirection
        );

        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);

        Page<Author> page;
        if (course != null && !course.isEmpty() && authorId == null) {
            // Use specification for course filtering
            Specification<Author> specification = AuthorSpecification.byCourse(course);
            page = authorRepository.findAll(specification, paging);
        } else if (authorId != null) {
            // Filter by authorId using specification
            Specification<Author> specification = AuthorSpecification.hasId(authorId);
            page = authorRepository.findAll(specification, paging);
        } else {
            // No filters
            page = authorRepository.findAll(paging);
        }

        List<AuthorDto> content = page.getContent().stream()
                .map(a -> getAuthorDTO(a.getId(), course))
                .toList();

        return new PageImpl<>(content, paging, page.getTotalElements());
    }

    /**
     * Filters authors with pagination and sorting.
     * Implementation for author-sd.md Section 2.1.1
     *
     * @param filterInput the filter criteria including page, pageSize, course, authorId
     * @return AuthorFilterDto with paginated results and filter metadata
     */
    public AuthorFilterDto filterAuthors(com.unitbv.myquiz.api.dto.AuthorFilterInputDto filterInput) {
        log.info("Filtering authors with input: {}", filterInput);

        // Apply defaults for null parameters
        int pageNo = filterInput.getPage() != null && filterInput.getPage() > 0
            ? filterInput.getPage() : 1;
        int pageSize = filterInput.getPageSize() != null && filterInput.getPageSize() > 0
            ? filterInput.getPageSize() : ControllerSettings.PAGE_SIZE;
        String sortField = "name"; // Default sort field
        String sortDirection = "asc"; // Default sort direction

        // Call the existing pagination method
        Page<AuthorDto> page = findPaginatedFiltered(
            filterInput.getCourse(),
            filterInput.getAuthorId(),
            pageNo,
            pageSize,
            sortField,
            sortDirection
        );

        // Build the response DTO
        AuthorFilterDto result = new AuthorFilterDto();
        result.setAuthors(page.getContent());
        result.setPageNo(pageNo);
        result.setTotalPages(page.getTotalPages());
        result.setTotalItems(page.getTotalElements());
        result.setCourses(getCourseNames());
        result.setSelectedCourse(filterInput.getCourse());

        // Populate authorList with distinct authors based on course filter
        // If course is selected, get authors for that course; otherwise get all authors
        List<AuthorInfo> authorList;
        String courseTrimmed = filterInput.getCourse() != null ? filterInput.getCourse().trim() : null;
        if (courseTrimmed != null && !courseTrimmed.isEmpty() && !"All Courses".equalsIgnoreCase(courseTrimmed)) {
            // Get distinct authors for the selected course (cached)
            authorList = getAuthorsByCourse(courseTrimmed);
        } else {
            // Get all distinct authors (no course filter or "All Courses" selected)
            authorList = getAllAuthorsBasic();
        }
        result.setAuthorList(authorList);

        log.info("Filtered {} authors, total: {}, pages: {}, authorList size: {}",
            page.getContent().size(), page.getTotalElements(), page.getTotalPages(), authorList.size());

        return result;
    }

    /**
     * Updates an existing author.
     * Implementation for author-sd.md Section 2.1.3
     *
     * @param id the author ID to update
     * @param authorDto the updated author data
     * @return the updated AuthorDto
     * @throws jakarta.persistence.EntityNotFoundException if author not found
     */
    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        log.info("Updating author with id: {}", id);

        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Author with id " + id + " not found"));

        // Update fields
        author.setName(authorDto.getName());
        author.setInitials(authorDto.getInitials());

        // Save and return
        Author updated = authorRepository.save(author);

        log.info("Successfully updated author id: {}, name: {}", updated.getId(), updated.getName());

        return new AuthorDto(updated.getId(), updated.getName(), updated.getInitials());
    }

    /**
     * Deletes an author by ID.
     * Implementation for author-sd.md Section 2.1.2
     * Throws EntityNotFoundException if author doesn't exist.
     *
     * @param id the author ID
     * @throws jakarta.persistence.EntityNotFoundException if author not found
     */
    public void deleteAuthor(Long id) {
        log.info("Deleting author with id: {}", id);

        if (!authorRepository.existsById(id)) {
            log.error("Author with id '{}' not found", id);
            throw new jakarta.persistence.EntityNotFoundException("Author with id " + id + " not found");
        }

        authorRepository.deleteById(id);
        log.info("Successfully deleted author with id: {}", id);
    }


    public boolean authorNameExists(String name) {
        Specification<Author> spec = AuthorSpecification.byName(name);
        return authorRepository.findOne(spec).isPresent();
    }


    public AuthorDto getAuthorByName(String name) {
        Specification<Author> spec = AuthorSpecification.byName(name);
        Author author = authorRepository.findOne(spec).orElse(null);
        if (author != null) {
            return new AuthorDto(author.getId(), author.getName(), author.getInitials());
        } else {
            return null;
        }
    }

    public AuthorDto getAuthorDtoWithAllQuizzes(String name) {
        Specification<Author> spec = AuthorSpecification.byName(name);
        Author author = authorRepository.findOne(spec).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        getQuizAuthors(author).forEach(quizAuthor -> {
            long noMC = getQuizAuthorQuestions(quizAuthor).stream().filter(AuthorService::testIfMultichoice).count();
            authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
            long noTF = getQuizAuthorQuestions(quizAuthor).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE)).count();
            authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
            authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + getQuizAuthorQuizErrors(quizAuthor).size());
            authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
            Quiz quizAuthorQuiz = getQuizAuthorQuiz(quizAuthor);
            if (quizAuthorQuiz == null) {
                return;
            }
            authorDto.setQuizName(quizAuthorQuiz.getName());
            authorDto.setTemplateType(quizAuthor.getTemplateType() != null ? quizAuthor.getTemplateType().toString() : TemplateType.Other.toString());
            authorDto.setCourse(quizAuthorQuiz.getCourse());
        });
        return authorDto;
    }

    private Quiz getQuizAuthorQuiz(QuizAuthor quizAuthor) {
        if (quizAuthor == null) {
            return null;
        }
        return quizRepository.findOne(
            QuizSpecification.byQuizAuthorId(quizAuthor.getId())
        ).orElse(null);
    }

    private List<QuizError> getQuizAuthorQuizErrors(QuizAuthor quizAuthor) {
        // Use specification with eager fetching to avoid N+1 queries
        Specification<QuizError> spec = QuizErrorSpecification.byQuizAuthor(quizAuthor.getId())
                .and(QuizErrorSpecification.fetchQuizAuthorWithDetails());
        return quizErrorRepository.findAll(spec);
    }

    private List<Question> getQuizAuthorQuestions(QuizAuthor quizAuthor) {
        // Use the new clear specification method for filtering by quizAuthorId
        Specification<Question> spec = QuestionSpecification.byQuizAuthorId(quizAuthor.getId());
        return questionRepository.findAll(spec);
    }

    private List<QuizAuthor> getQuizAuthors(Author author) {
        return quizAuthorRepository.findAll(
                QuizAuthorSpecification.hasAuthorId(author.getId())
        );
    }

    /**
     * Gets all unique course names from quizzes, sorted case-insensitively.
     *
     * @return List of unique course names
     */
    public List<String> getCourseNames() {
        return quizRepository.findAll().stream()
                .map(Quiz::getCourse)
                .filter(course -> course != null && !course.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }


    public void deleteAuthorsWithoutQuiz() {
        List<Author> authorsList = authorRepository.findAll();
        authorsList.forEach(author -> {
            if (getQuizAuthors(author).isEmpty()) {
                authorRepository.delete(author);
            }
        });
    }

    public List<String> getCoursesNamesAsStrings() {
        return new ArrayList<>(getCourseNames());
    }


    public AuthorFilterDto getFilteredAuthors(String selectedCourse) {
        AuthorFilterDto authorFilterDto = new AuthorFilterDto();
        List<AuthorDto> authorDtos = new ArrayList<>();

        List<String> courses = getCoursesNamesAsStrings();

        if (selectedCourse == null) {
            selectedCourse = !courses.isEmpty() ? courses.getFirst() : "";
        }

        int pageNo = 1;
        Page<AuthorDto> page = findPaginatedFiltered(selectedCourse, null, pageNo, ControllerSettings.PAGE_SIZE, "name", "desc");
        String finalSelectedCourse = selectedCourse;
        page.stream().forEach(authorDto -> authorDtos.add(getAuthorDTO(authorDto.getId(), finalSelectedCourse)));

        authorFilterDto.setCourses(courses);
        authorFilterDto.setPageNo(pageNo);
        authorFilterDto.setTotalPages(page.getTotalPages());
        authorFilterDto.setTotalItems(page.getTotalElements());
        authorFilterDto.setAuthors(authorDtos);
        authorFilterDto.setSelectedCourse(selectedCourse);
        return authorFilterDto;
    }

    public AuthorDataDto prepareAuthorData(String authorName) {
        AuthorDataDto authorDataDto = new AuthorDataDto();
        AuthorDto author = getAuthorByName(authorName);
        AuthorDto authorDTO = getAuthorDtoWithAllQuizzes(author.getName());

        List<QuizDto> quizDtos = new ArrayList<>();
        quizAuthorService.getQuizAuthorsForAuthorName(authorName).forEach(quizAuthor -> {
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

        quizDtos.sort(quizService::getCompareTo);

        quizDtos.forEach(quizDto -> {
            List<QuestionDto> questionDtos = new ArrayList<>();
            List<QuestionDto> questionDtosTF = new ArrayList<>();
            List<Question> quizQuestions = questionService.getQuizzQuestionsForAuthor(quizDto.getQuizAuthorId());
            quizQuestions.forEach(question -> {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    QuestionDto dto = questionMapper.toDto(question);
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
            List<AuthorErrorDto> authorErrorDtos = quizErrorService.getErrorsForQuizAuthor(quizDto.getQuizAuthorId());
            authorErrorDtos = quizErrorService.sortAuthorErrorsByRow(authorErrorDtos);

            quizDto.setQuestionDtosMultichoice(questionDtos);
            quizDto.setQuestionDtosTruefalse(questionDtosTF);
            quizDto.setAuthorErrorDtos(authorErrorDtos);
            quizDto.setQuestionsMultichoice(questionDtos);
            quizDto.setQuestionsTruefalse(questionDtosTF);
        });

        authorDataDto.setQuizDtos(quizDtos);
        authorDataDto.setQuizDto(!quizDtos.isEmpty() ? quizDtos.getFirst() : null);
        List<AuthorInfo> authorsList = getAuthorsByCourse(!quizDtos.isEmpty() ? quizDtos.getFirst().getCourse() : "");

        authorDataDto.setAuthorsList(authorsList);
        authorDataDto.setAuthorDTO(authorDTO);
        return authorDataDto;
    }


    @Cacheable(value = "authorsByCourse", key = "#course")
    public List<AuthorInfo> getAuthorsByCourse(String course) {
        // Use Specification pattern with optimized query to fetch distinct authors for a course
        // This creates lightweight DTOs with only id, name, and initials
        return quizAuthorRepository.findAll(
                QuizAuthorSpecification.hasCourse(course)
                    .and(QuizAuthorSpecification.fetchAuthor())
        ).stream()
            .map(QuizAuthor::getAuthor)
            .distinct()
            .map(a -> new AuthorInfo(a.getId(), a.getName()))
            .toList();
    }


    public AuthorDto getAuthorById(Long selectedAuthorObj) {
        if (selectedAuthorObj == null) {
            return null;
        }
        Author author = authorRepository.findById(selectedAuthorObj).orElse(null);
        if (author == null) {
            return null;
        }
        return new AuthorDto(author.getId(), author.getName(), author.getInitials());
    }

    public Author findAuthorEntityById(Long id) {
        return authorRepository.findById(id).orElse(null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AuthorDetailsDto getAuthorDetails(Long id) {
        Author author = authorRepository.findById(id).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        // Get all quizzes for this author
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasAuthorId(id)
        );
        List<QuizDto> quizzes = new ArrayList<>();
        Map<Long, List<QuestionDto>> questionsByQuiz = new java.util.HashMap<>();
        Map<Long, List<AuthorErrorDto>> errorsByQuiz = new java.util.HashMap<>();
        for (QuizAuthor qa : quizAuthors) {
            Quiz quiz = quizRepository.findOne(
                QuizSpecification.byQuizAuthorId(qa.getId())
            ).orElse(null);
            if (quiz == null) continue;
            QuizDto quizDto = new QuizDto();
            quizDto.setId(quiz.getId());
            quizDto.setName(quiz.getName());
            quizDto.setYear(quiz.getYear());
            quizDto.setCourse(quiz.getCourse());
            quizzes.add(quizDto);
            // Questions for this quiz and author - use the new clear specification method
            Specification<Question> spec = QuestionSpecification.byQuizAuthorId(qa.getId());
            List<Question> filteredQuestions = questionRepository.findAll(spec);
            List<QuestionDto> questionDtos = filteredQuestions.stream().map(questionMapper::toDto).toList();
            questionsByQuiz.put(quiz.getId(), questionDtos);
            // Errors for this quiz and author
            List<AuthorErrorDto> errorDtos = quizErrorService.getErrorsByQuizAndAuthor(quiz.getId(), id);
            errorsByQuiz.put(quiz.getId(), errorDtos);
        }
        AuthorDetailsDto details = new AuthorDetailsDto();
        details.setAuthor(authorDto);
        details.setQuizzes(quizzes);
        details.setQuestionsByQuiz(questionsByQuiz);
        details.setErrorsByQuiz(errorsByQuiz);
        return details;
    }

}
