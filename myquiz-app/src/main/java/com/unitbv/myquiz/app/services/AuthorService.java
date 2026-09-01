package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.AuthorFormDataDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseInfo;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.AuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankSpecification;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthorService {
    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);

    /** Name of the fallback dummy author used for unresolved initials during import. */
    public static final String DUMMY_AUTHOR_NAME = "Unknown Author";
    /** Initials of the fallback dummy author. */
    public static final String DUMMY_AUTHOR_INITIALS = "NA";

    // All dependencies are final for thread safety
    private final AuthorRepository authorRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionErrorService questionErrorService;
    private final QuestionBankAuthorService questionBankAuthorService;
    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final QuestionMapper questionMapper;
    private final CourseService courseService;

    // Self-reference for calling @Cacheable methods (enables cache proxy)
    private final AuthorService self;

    @Lazy
    @Autowired
    public AuthorService(AuthorRepository authorRepository, QuestionRepository questionRepository, QuestionService questionService, QuestionBankAuthorRepository questionBankAuthorRepository,
                         QuestionBankRepository questionBankRepository, QuestionErrorService questionErrorService, QuestionBankAuthorService questionBankAuthorService,
                         QuestionErrorRepository questionErrorRepository, QuestionDuplicateRepository questionDuplicateRepository,
                         QuestionMapper questionMapper, CourseService courseService, @Lazy AuthorService self) {
        this.authorRepository = authorRepository;
        this.questionRepository = questionRepository;
        this.questionService = questionService;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionErrorService = questionErrorService;
        this.questionBankAuthorService = questionBankAuthorService;
        this.questionErrorRepository = questionErrorRepository;
       this.questionDuplicateRepository = questionDuplicateRepository;
       this.questionMapper = questionMapper;
       this.courseService = courseService;
       this.self = self != null ? self : this;
    }

    private AuthorService getSelf() {
        return self != null ? self : this;
    }

    public String extractAuthorNameFromPath(String filePath) {
        // Input validation
        if (filePath == null || filePath.trim().isEmpty()) {
            log.atWarn().log("File path is null or empty");
            return MyUtil.USER_NAME_NOT_DETECTED;
        }

        String authorName = MyUtil.USER_NAME_NOT_DETECTED;

        Path path = Paths.get(filePath);
        if (FileValidator.exists(path)) {
            Path parent = path.getParent();
            if (parent != null && parent.getFileName() != null) {
                String lastDirectory = parent.getFileName().toString();
                int endIndex = lastDirectory.indexOf("_");
                if (endIndex != -1) {
                    authorName = lastDirectory.substring(0, endIndex);
                } else {
                    log.atError().addArgument(lastDirectory).addArgument(authorName).log("Directory name '{}' not in the correct format (e.g.: 'John Doe_123'), use default '{}'");
                }
            }
        } else {
            log.atError().addArgument(filePath).log("Directory not found: {}");
        }
        return authorName;
    }

    public String extractInitials(String authorName) {
        // Input validation
        if (authorName == null || authorName.trim().isEmpty()) {
            return "";
        }

        StringBuilder initials = new StringBuilder();
        String[] split = authorName.split(" ");
        for (String s : split) {
            if (!s.isEmpty()) {
                initials.append(s.charAt(0));
            }
        }
        return initials.toString();
    }

    @Transactional
    @CacheEvict(value = {"allAuthorsBasic", "authorsByCourse"}, allEntries = true)
    public AuthorDto saveAuthorDto(AuthorDto authorDto) {
        // Input validation
        if (authorDto == null) {
            log.atWarn().log("AuthorDto is null");
            return null;
        }
        if (authorDto.getName() == null || authorDto.getName().trim().isEmpty()) {
            log.atWarn().log("Author name is null or empty");
            return null;
        }

        AuthorDto dto;
        if (authorDto.getId() == null) {
            Specification<Author> spec = AuthorSpecification.byName(authorDto.getName());
            Author existingAuthor = authorRepository.findOne(spec).orElse(null);
            if (existingAuthor != null) {
                log.atInfo().addArgument(authorDto.getName()).addArgument(existingAuthor.getId()).log("Author with name '{}' already exists with id '{}'");
                dto = mapToAuthorDto(existingAuthor);
            } else {
                log.atInfo().addArgument(authorDto.getName()).log("No existing author found with name '{}', proceeding to create new author");
                Author author = new Author();
                author.setName(authorDto.getName());
                author.setInitials(authorDto.getInitials());
                authorRepository.save(author);
                dto = mapToAuthorDto(author);
            }
        } else {
            Author author = authorRepository.findById(authorDto.getId()).orElse(null);
            if (author != null) {
                author.setName(authorDto.getName());
                author.setInitials(authorDto.getInitials());
                authorRepository.save(author);
                dto = mapToAuthorDto(author);
            } else {
                // ID provided but not found; create new
                Author newAuthor = new Author();
                newAuthor.setName(authorDto.getName());
                newAuthor.setInitials(authorDto.getInitials());
                authorRepository.save(newAuthor);
                dto = mapToAuthorDto(newAuthor);
            }
        }
        return dto;
    }

    public List<AuthorDto> getAllAuthors() {
        try {
            return authorRepository.findAll().stream().map(a -> {
                AuthorDto dto = mapToAuthorDto(a);
                // Compute question counts for each author
                long mcCount = 0L;
                long tfCount = 0L;
                List<QuestionBankAuthor> questionBankAuthors = getQuestionBankAuthors(a);
                for (QuestionBankAuthor qa : questionBankAuthors) {
                    mcCount += getQuestionBankAuthorQuestions(qa).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.MULTICHOICE)).count();
                    tfCount += getQuestionBankAuthorQuestions(qa).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE)).count();
                }
                long totalCount = mcCount + tfCount;
                dto.setNumberOfMultipleChoiceQuestions(mcCount);
                dto.setNumberOfTrueFalseQuestions(tfCount);
                dto.setNumberOfQuestions(totalCount);

                // Count questions with duplicate links
                List<Long> questionIds = questionBankAuthors.stream()
                        .flatMap(qa -> getQuestionBankAuthorQuestions(qa).stream())
                        .map(Question::getId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
                long duplicatesCount = 0L;
                if (!questionIds.isEmpty()) {
                    for (Long qid : questionIds) {
                        if (questionDuplicateRepository.countByQuestionIdOrDuplicateQuestionId(qid, qid) > 0) {
                            duplicatesCount++;
                        }
                    }
                }
                dto.setNumberOfDuplicates(duplicatesCount);
                return dto;
            }).toList();
        } catch (Exception e) {
            log.atError().addArgument(e.getMessage()).log("Error getting authors: {}");
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
            return authorRepository.findAll().stream().map(this::mapToAuthorInfo).toList();
        } catch (Exception e) {
            log.atError().addArgument(e.getMessage()).log("Error getting authors: {}");
            return Collections.emptyList();
        }
    }

    public boolean existsById(Long id) {
        return authorRepository.existsById(id);
    }

    @Transactional
    @CacheEvict(value = {"allAuthorsBasic", "authorsByCourse"}, allEntries = true)
    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = {"allAuthorsBasic", "authorsByCourse"}, allEntries = true)
    public void deleteAll() {
        authorRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public AuthorDto getAuthorWithQuestionBankStats(Long authorId, String courseName) {
        if (authorId == null) return null;
        Author author = authorRepository.findById(authorId).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = mapToAuthorDto(author);

        long mcCount = 0L;
        long tfCount = 0L;
        long errorCount = 0L;
        Set<Long> questionIds = new HashSet<>();

        for (QuestionBankAuthor questionBankAuthor : getQuestionBankAuthors(author)) {
            QuestionBankAuthorStats stats = collectQuestionBankAuthorStats(questionBankAuthor, courseName);
            if (stats != null) {
                mcCount += stats.mcCount();
                tfCount += stats.tfCount();
                errorCount += stats.errorCount();
                questionIds.addAll(stats.questionIds());

                authorDto.setQuestionBankName(stats.questionBank().getName());
                authorDto.setTemplateType(questionBankAuthor.getTemplateType() != null ? questionBankAuthor.getTemplateType().toString() : TemplateType.Other.toString());
                authorDto.setCourse(stats.questionBank().getCourseName());

                log.atInfo().log("Author '{}', QuestionBank '{}', Course '{}': MCQ count = {}, TF count = {}", author.getName(), stats.questionBank().getName(), stats.questionBank().getCourseName(), stats.mcCount(), stats.tfCount());
            }
        }

        authorDto.setNumberOfMultipleChoiceQuestions(mcCount);
        authorDto.setNumberOfTrueFalseQuestions(tfCount);
        authorDto.setNumberOfQuestions(mcCount + tfCount);
        authorDto.setNumberOfErrors(errorCount);

        long duplicatesCount = 0L;
        for (Long questionId : questionIds) {
            if (questionDuplicateRepository.countByQuestionIdOrDuplicateQuestionId(questionId, questionId) > 0) {
                duplicatesCount++;
            }
        }
        authorDto.setNumberOfDuplicates(duplicatesCount);

        return authorDto;
    }

    private QuestionBankAuthorStats collectQuestionBankAuthorStats(QuestionBankAuthor questionBankAuthor, String courseName) {
        QuestionBank questionBank = questionBankAuthor.getQuestionBank();
        if (questionBank == null) {
            questionBank = getQuestionBankFromAuthorLink(questionBankAuthor);
        }
        boolean includeByCourse = questionBank != null
                && ((courseName == null || courseName.isBlank())
                || (questionBank.getCourse() != null
                && questionBank.getCourseName().equalsIgnoreCase(courseName)));
        if (!includeByCourse) {
            return null;
        }

        List<Question> questions = getQuestionBankAuthorQuestions(questionBankAuthor);
        long noMC = questions.stream()
                .filter(q -> q.getType() != null && q.getType().equals(QuestionType.MULTICHOICE))
                .count();
        long noTF = questions.stream().filter(this::testIfTruefalse).count();
        long errorCount = getQuestionBankAuthorQuestionErrors(questionBankAuthor).size();
        Set<Long> questionIds = new HashSet<>();
        questions.stream().map(Question::getId).filter(java.util.Objects::nonNull).forEach(questionIds::add);
        return new QuestionBankAuthorStats(questionBank, noMC, noTF, errorCount, questionIds);
    }

    private record QuestionBankAuthorStats(QuestionBank questionBank, long mcCount, long tfCount, long errorCount, Set<Long> questionIds) {
    }

    private boolean testIfTruefalse(Question q) {
        return q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE);
    }

    public Page<AuthorDto> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        Page<Author> page = authorRepository.findAll(paging);
        List<AuthorDto> content = page.getContent().stream().map(this::mapToAuthorDto).toList();
        return new PageImpl<>(content, paging, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AuthorDto> findPaginatedFiltered(String course, Long authorId, Long questionBankId, int pageNo, int pageSize, String sortField, String sortDirection) {
        log.atInfo().log(
                "Finding paginated filtered authors - course: '{}', authorId: '{}', questionBankId: '{}', pageNo: {}, pageSize: {}, sortField: {}, sortDirection: {}", course, authorId,
                questionBankId, pageNo, pageSize, sortField, sortDirection
        );

        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);

        Page<Author> page;
        if (course != null && !course.isBlank() && authorId == null && questionBankId == null) {
            // Use specification for course filtering
            Specification<Author> specification = AuthorSpecification.byCourse(course);
            page = authorRepository.findAll(specification, paging);
        } else if (questionBankId != null) {
            // Filter by QuestionBank using specification
            Specification<Author> specification = AuthorSpecification.byQuestionBank(questionBankId);
            page = authorRepository.findAll(specification, paging);
        } else if (authorId != null) {
            // Filter by authorId using specification
            Specification<Author> specification = AuthorSpecification.hasId(authorId);
            page = authorRepository.findAll(specification, paging);
        } else {
            // No filters
            page = authorRepository.findAll(paging);
        }

        List<AuthorDto> content = page.getContent().stream().map(a -> getSelf().getAuthorWithQuestionBankStats(a.getId(), course)).toList();

        return new PageImpl<>(content, paging, page.getTotalElements());
    }

    /**
     * Updates an existing author.
     *
     * @param id        the author ID to update
     * @param authorDto the updated author data
     * @return updated AuthorDto
     * @throws jakarta.persistence.EntityNotFoundException if author not found
     */
    @Transactional
    @CacheEvict(value = {"allAuthorsBasic", "authorsByCourse"}, allEntries = true)
    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        log.atInfo().addArgument(id).log("Updating author with id: {}");

        if (authorDto == null) {
            throw new IllegalArgumentException("AuthorDto cannot be null");
        }

        Author author = authorRepository.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Author with id " + id + " not found"));

        // Update fields
        author.setName(authorDto.getName());
        author.setInitials(authorDto.getInitials());

        // Save and return
        Author updated = authorRepository.save(author);

        log.atInfo().addArgument(updated.getId()).addArgument(updated.getName()).log("Successfully updated author id: {}, name: {}");

        return mapToAuthorDto(updated);
    }

    /**
     * Deletes an author by ID.
     * Throws EntityNotFoundException if author doesn't exist.
     *
     * @param id the author ID
     * @throws jakarta.persistence.EntityNotFoundException if author not found
     */
    @Transactional
    @CacheEvict(value = {"allAuthorsBasic", "authorsByCourse"}, allEntries = true)
    public void deleteAuthor(Long id) {
        log.atInfo().addArgument(id).log("Deleting author with id: {}");

        if (!authorRepository.existsById(id)) {
            log.atError().addArgument(id).log("Author with id '{}' not found");
            throw new jakarta.persistence.EntityNotFoundException("Author with id " + id + " not found");
        }

        authorRepository.deleteById(id);
        log.atInfo().addArgument(id).log("Successfully deleted author with id: {}");
    }

    public boolean authorNameExists(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        Specification<Author> spec = AuthorSpecification.byName(name);
        return authorRepository.findOne(spec).isPresent();
    }

    public AuthorDto getAuthorByName(String name) {
        // Input validation
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Specification<Author> spec = AuthorSpecification.byName(name);
        Author author = authorRepository.findOne(spec).orElse(null);
        if (author != null) {
            return mapToAuthorDto(author);
        } else {
            return null;
        }
    }

    public AuthorDto getAuthorDtoWithAllQuestionBanks(String name) {
        // Input validation
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Specification<Author> spec = AuthorSpecification.byName(name);
        Author author = authorRepository.findOne(spec).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = mapToAuthorDto(author);
        getQuestionBankAuthors(author).forEach(questionBankAuthor -> {
            long noMC = getQuestionBankAuthorQuestions(questionBankAuthor).stream()
                    .filter(q -> q.getType() != null && q.getType().equals(QuestionType.MULTICHOICE))
                    .count();
            authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions() + noMC);
            long noTF = getQuestionBankAuthorQuestions(questionBankAuthor).stream().filter(q -> q.getType() != null && q.getType().equals(QuestionType.TRUEFALSE)).count();
            authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions() + noTF);
            authorDto.setNumberOfErrors(authorDto.getNumberOfErrors() + getQuestionBankAuthorQuestionErrors(questionBankAuthor).size());
            authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
            QuestionBank questionBank = getQuestionBankFromAuthorLink(questionBankAuthor);
            if (questionBank == null) {
                return;
            }
            authorDto.setQuestionBankName(questionBank.getName());
            authorDto.setTemplateType(questionBankAuthor.getTemplateType() != null ? questionBankAuthor.getTemplateType().toString() : TemplateType.Other.toString());
            authorDto.setCourse(questionBank.getCourseName());
        });
        return authorDto;
    }

    private QuestionBank getQuestionBankFromAuthorLink(QuestionBankAuthor questionBankAuthor) {
        if (questionBankAuthor == null) {
            return null;
        }
        return questionBankRepository.findOne(QuestionBankSpecification.byQuestionBankAuthorId(questionBankAuthor.getId())).orElse(null);
    }

    private List<QuestionError> getQuestionBankAuthorQuestionErrors(QuestionBankAuthor questionBankAuthor) {
        return questionErrorRepository.findByQuestionQuestionBankAuthorId(questionBankAuthor.getId())
                .stream()
                .filter(error -> !MyUtil.isDuplicateValidationError(error.getDescription()))
                .toList();
    }

    private List<Question> getQuestionBankAuthorQuestions(QuestionBankAuthor questionBankAuthor) {
        Specification<Question> spec = QuestionSpecification.byQuestionBankAuthorId(questionBankAuthor.getId());
        return questionRepository.findAll(spec);
    }

    private List<QuestionBankAuthor> getQuestionBankAuthors(Author author) {
        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasAuthorId(author.getId()));
    }

    /**
     * Gets all unique course names from QuestionBanks, sorted case-insensitively.
     *
     * @return List of unique course names
     */
    @Transactional(readOnly = true)
    public List<String> getCourseNames() {
        return questionBankRepository.findAll().stream().map(QuestionBank::getCourseName).filter(course -> course != null && !course.isEmpty()).distinct().sorted(String::compareToIgnoreCase).toList();
    }

    @Transactional
    public void deleteAuthorsWithoutQuestionBank() {
        List<Author> authorsList = authorRepository.findAll();
        authorsList.forEach(author -> {
            if (getQuestionBankAuthors(author).isEmpty()) {
                authorRepository.delete(author);
            }
        });
    }

    public AuthorFilterResponseDto getFilteredAuthors(String selectedCourse) {
        AuthorFilterResponseDto authorFilterDto = new AuthorFilterResponseDto();
        List<AuthorDto> authorDtos = new ArrayList<>();

        List<CourseInfo> courses = courseService.getAllCourses().stream().map(CourseInfo::from).toList();

        if (selectedCourse == null) {
            selectedCourse = !courses.isEmpty() ? courses.getFirst().getName() : "";
        }

        int pageNo = 1;
        Page<AuthorDto> page = getSelf().findPaginatedFiltered(selectedCourse, null, null, pageNo, ControllerSettings.PAGE_SIZE, "name", "desc");
        String finalSelectedCourse = selectedCourse;
        page.stream().forEach(authorDto -> authorDtos.add(
                getSelf().getAuthorWithQuestionBankStats(authorDto.getId(), finalSelectedCourse)));

        authorFilterDto.setCourses(courses);
        authorFilterDto.setPage(pageNo);
        authorFilterDto.setTotalPages(page.getTotalPages());
        authorFilterDto.setTotalElements(page.getTotalElements());
        authorFilterDto.setAuthors(authorDtos);
        authorFilterDto.setSelectedCourse(selectedCourse);
        return authorFilterDto;
    }

    public AuthorFormDataDto prepareAuthorData(String authorName) {
        // Input validation
        if (authorName == null || authorName.trim().isEmpty()) {
            log.atWarn().log("Author name is null or empty");
            return new AuthorFormDataDto();
        }

        AuthorFormDataDto authorDataDto = new AuthorFormDataDto();
        AuthorDto author = getAuthorByName(authorName);
        if (author == null) {
            log.atWarn().addArgument(authorName).log("Author not found: {}");
            return new AuthorFormDataDto();
        }
        AuthorDto authorDto = getAuthorDtoWithAllQuestionBanks(author.getName());

        List<QuestionBankDto> questionBankDtos = new ArrayList<>();
        questionBankAuthorService.getQuestionBankAuthorsForAuthorName(authorName).forEach(questionBankAuthor -> {
            QuestionBank questionBank = getQuestionBankFromAuthorLink(questionBankAuthor);
            if (questionBank == null) {
                return;
            }
            QuestionBankDto questionBankDto = new QuestionBankDto();
            questionBankDto.setId(questionBank.getId());
            questionBankDto.setName(questionBank.getName());
            questionBankDto.setCourse(questionBank.getCourseName());
            questionBankDto.setStudyYear(questionBank.getStudyYear());
            questionBankDto.setSourceFile(questionBankAuthor.getSource());
            questionBankDto.setQuestionBankAuthorId(questionBankAuthor.getId());
            questionBankDtos.add(questionBankDto);
        });

        questionBankDtos.sort(Comparator.comparing(
                dto -> dto != null ? dto.getCourse() : null,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));

        questionBankDtos.forEach(questionBankDto -> {
            List<QuestionDto> questionDtos = new ArrayList<>();
            List<QuestionDto> questionDtosTF = new ArrayList<>();
            List<Question> questionList = questionService.getQuestionBankQuestionsForAuthor(questionBankDto.getQuestionBankAuthorId());
            questionList.forEach(question -> {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    QuestionDto dto = questionMapper.toDto(question);
                    questionDtos.add(dto);
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    QuestionDto dto = questionMapper.toDto(question);
                    questionDtosTF.add(dto);
                }
            });
            List<QuestionErrorDto> questionErrorDtos = questionErrorService.getErrorsForQuestionBankAuthor(questionBankDto.getQuestionBankAuthorId());

            questionBankDto.setQuestionErrorDtos(questionErrorDtos);
            questionBankDto.setQuestionsMultichoice(questionDtos);
            questionBankDto.setQuestionsTruefalse(questionDtosTF);
        });

        authorDataDto.setQuestionBankDtos(questionBankDtos);
        // Use self-reference for cache to work
        List<AuthorInfo> authorsList = getSelf().getAuthorsByCourse(!questionBankDtos.isEmpty() ? questionBankDtos.getFirst().getCourse() : "");

        authorDataDto.setAuthorsList(authorsList);
        authorDataDto.setAuthor(authorDto);
        return authorDataDto;
    }

    @Cacheable(value = "authorsByCourse", key = "#course")
    public List<AuthorInfo> getAuthorsByCourse(String course) {
        // Use Specification pattern with optimized query to fetch distinct authors for a course
        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasCourse(course).and(QuestionBankAuthorSpecification.fetchAuthor())).stream().map(QuestionBankAuthor::getAuthor)
                                           .distinct().map(this::mapToAuthorInfo).toList();
    }

    public AuthorDto getAuthorById(Long selectedAuthorObj) {
        if (selectedAuthorObj == null) {
            return null;
        }
        Author author = authorRepository.findById(selectedAuthorObj).orElse(null);
        if (author == null) {
            return null;
        }
        return mapToAuthorDto(author);
    }

    public Author findAuthorEntityById(Long id) {
        return authorRepository.findById(id).orElse(null);
    }

    /**
     * Save author from file path - extract name and create if doesn't exist.
     *
     * @param file The file containing author information in its path
     * @return Saved Author entity or null if extraction fails
     */
    @Transactional
    public Author saveAuthorFromFile(java.io.File file) {
        // Input validation
        if (file == null) {
            log.atWarn().log("File is null");
            return null;
        }

        String authorName = extractAuthorNameFromPath(file.getAbsolutePath());
        String initials = extractInitials(authorName);

        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(authorName);
        authorDto.setInitials(initials);

        if (authorNameExists(authorDto.getName())) {
            log.atInfo().addArgument(authorDto.getName()).log("Author {} already exists in the database");
            authorDto = getAuthorByName(authorDto.getName());
        } else {
            authorDto = getSelf().saveAuthorDto(authorDto);
        }

        if (authorDto == null) {
            return null;
        }

        return authorRepository.findById(authorDto.getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public AuthorDetailsDto getAuthorDetails(Long id) {
        Author author = authorRepository.findById(id).orElse(null);
        if (author == null) return null;
        AuthorDto authorDto = mapToAuthorDto(author);

        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasAuthorId(id));
        List<QuestionBankDto> questionBanks = new ArrayList<>();
        Map<Long, List<QuestionDto>> questionsByQuestionBank = new java.util.HashMap<>();
        Map<Long, List<QuestionErrorDto>> errorsByQuestionBank = new java.util.HashMap<>();
        for (QuestionBankAuthor qa : questionBankAuthors) {
            QuestionBank questionBank = questionBankRepository.findOne(QuestionBankSpecification.byQuestionBankAuthorId(qa.getId())).orElse(null);
            if (questionBank != null) {
                QuestionBankDto questionBankDto = new QuestionBankDto();
                questionBankDto.setId(questionBank.getId());
                questionBankDto.setName(questionBank.getName());
                questionBankDto.setStudyYear(questionBank.getStudyYear());
                questionBankDto.setCourse(questionBank.getCourseName());
                questionBanks.add(questionBankDto);
                // Questions for this questionBank and author
                Specification<Question> spec = QuestionSpecification.byQuestionBankAuthorId(qa.getId());
                List<Question> filteredQuestions = questionRepository.findAll(spec);
                List<QuestionDto> questionDtos = filteredQuestions.stream().map(questionMapper::toDto).toList();
                questionsByQuestionBank.put(questionBank.getId(), questionDtos);
                // Errors for this questionBank and author
                List<QuestionErrorDto> errorDtos = questionErrorService.getErrorsByQuestionBankAndAuthor(questionBank.getId(), id);
                errorsByQuestionBank.put(questionBank.getId(), errorDtos);
            }
        }
        AuthorDetailsDto details = new AuthorDetailsDto();
        details.setAuthor(authorDto);
        details.setQuestionBanks(questionBanks);
        details.setQuestionsByQuestionBank(questionsByQuestionBank);
        details.setErrorsByQuestionBank(errorsByQuestionBank);
        return details;
    }

    /**
     * Helper method to map Author entity to AuthorDto.
     * Centralizes DTO mapping to eliminate code duplication.
     *
     * @param author the Author entity
     * @return AuthorDto with basic author information
     */
    private AuthorDto mapToAuthorDto(Author author) {
        if (author == null) {
            return null;
        }
        return AuthorDto.builder()
                .id(author.getId())
                .name(author.getName())
                .initials(author.getInitials())
                .build();
    }

    /**
     * Helper method to map Author entity to AuthorInfo.
     * Centralizes lightweight DTO mapping for dropdowns and filters.
     *
     * @param author the Author entity
     * @return AuthorInfo with id, name and initials
     */
    private AuthorInfo mapToAuthorInfo(Author author) {
        if (author == null) {
            return null;
        }
        return new AuthorInfo(author.getId(), author.getName(), author.getInitials());
    }

    /**
     * Finds an existing "Unknown Author" dummy entry or creates one.
     * Used to assign a single shared author to all records whose initials cannot be resolved.
     *
     * @return dummy Author entity (never null)
     */
    @Transactional
    public Author findOrCreateDummyAuthor() {
        Specification<Author> spec = AuthorSpecification.byName(DUMMY_AUTHOR_NAME);
        Author existing = authorRepository.findOne(spec).orElse(null);
        if (existing != null) {
            log.atInfo().log("Found existing dummy author '{}'", DUMMY_AUTHOR_NAME);
            return existing;
        }
        log.atInfo().log("Creating dummy author '{}' with initials '{}'", DUMMY_AUTHOR_NAME, DUMMY_AUTHOR_INITIALS);
        Author dummy = new Author(DUMMY_AUTHOR_NAME, DUMMY_AUTHOR_INITIALS);
        return authorRepository.save(dummy);
    }

}
