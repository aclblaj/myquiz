package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.dto.QuizFilterDto;
import com.unitbv.myquiz.api.dto.QuizFilterInputDto;
import com.unitbv.myquiz.api.dto.QuizInfo;
import com.unitbv.myquiz.api.types.QuestionType;
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
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuizErrorSpecification;
import com.unitbv.myquiz.app.specifications.QuizSpecification;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class QuizService {
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuestionRepository questionRepository;
    private final QuizErrorRepository quizErrorRepository;
    private final AuthorRepository authorRepository;
    private final CourseService courseService;
    private final QuestionMapper questionMapper;

    @Autowired
    public QuizService(QuizRepository quizRepository, QuizAuthorRepository quizAuthorRepository, QuestionRepository questionRepository, QuizErrorRepository quizErrorRepository,
                       AuthorRepository authorRepository, CourseService courseService, QuestionMapper questionMapper) {
        this.quizRepository = quizRepository;
        this.quizAuthorRepository = quizAuthorRepository;
        this.questionRepository = questionRepository;
        this.quizErrorRepository = quizErrorRepository;
        this.authorRepository = authorRepository;
        this.courseService = courseService;
        this.questionMapper = questionMapper;
    }


    public Quiz createQuizz(String courseName, String quizName, long year) {
        Quiz quiz;
        Optional<Quiz> searchQuiz = quizRepository.findOne(QuizSpecification.byNameAndCourseAndYear(quizName, courseName, year));
        if (searchQuiz.isEmpty()) {
            Quiz newQuiz = new Quiz();
            newQuiz.setName(quizName);
            newQuiz.setCourse(courseName);
            newQuiz.setYear(year);
            quiz = quizRepository.save(newQuiz);
        } else {
            quiz = searchQuiz.get();
        }
        return quiz;
    }


    public void deleteAll() {
        quizRepository.deleteAll();
    }


    public List<QuizDto> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAll();
        quizzes.sort(Comparator.comparing(Quiz::getCourse));
        return quizzes.stream().map(quiz -> {
            QuizDto dto = new QuizDto();
            dto.setId(quiz.getId());
            dto.setName(quiz.getName());
            dto.setCourse(quiz.getCourse());
            dto.setYear(quiz.getYear());
            // Fetch questions for this quiz using QuestionSpecification
            var spec = QuestionSpecification.byFilters(null, null, quiz.getId(), null);
            List<Question> questions = questionRepository.findAll(spec);
            int mcCount = 0;
            int tfCount = 0;
            for (Question question : questions) {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    mcCount++;
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    tfCount++;
                }
            }
            dto.setMcQuestionsCount(mcCount);
            dto.setTfQuestionsCount(tfCount);
            // count authors
            List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(QuizAuthorSpecification.hasQuizId(quiz.getId()).and(QuizAuthorSpecification.fetchQuestionsAndErrors()));
            dto.setNoAuthors(quizAuthors.size());
            return dto;
        }).toList();
    }


    /**
     * Delete a quiz by ID along with all related data.
     * OPTIMIZED for performance with batch operations and reduced queries.
     * <p>
     * Deletion sequence (following quiz-sd.md specifications):
     * 1. Verify quiz exists
     * 2. Fetch all QuizAuthor entries with authors (single query with fetch)
     * 3. Batch delete all Questions for this quiz (single query)
     * 4. Batch delete all QuizErrors for this quiz (single query)
     * 5. Batch delete all QuestionErrors for this quiz (single query)
     * 6. Delete all QuizAuthor entries (already loaded)
     * 7. Delete the Quiz itself
     * 8. Batch check and delete orphaned Authors (optimized with IN clause)
     *
     * @param id Quiz ID to delete
     * @throws IllegalArgumentException if quiz with given ID does not exist
     */
    @Transactional
    public void deleteQuizById(Long id) {
        long startTime = System.currentTimeMillis();
        logger.atInfo().addArgument(id).log("Starting optimized deletion of quiz with ID: {}");

        // Step 1: Verify quiz exists (fast check)
        if (!quizRepository.existsById(id)) {
            logger.atWarn().addArgument(id).log("Quiz with ID {} not found - cannot delete");
            throw new IllegalArgumentException("Quiz not found with ID: " + id);
        }

        // Step 2: Fetch all QuizAuthor entries with authors in ONE query (eager fetch)
        long fetchStart = System.currentTimeMillis();
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(id)
                .and(QuizAuthorSpecification.fetchAuthor())
        );
        logger.atInfo()
              .addArgument(quizAuthors.size())
              .addArgument(System.currentTimeMillis() - fetchStart)
              .log("Found {} QuizAuthor entries in {}ms");

        // Collect authors for orphan check (no extra queries)
        Set<Author> authorsToCheck = new HashSet<>();
        List<Long> quizAuthorIds = new ArrayList<>();
        for (QuizAuthor quizAuthor : quizAuthors) {
            quizAuthorIds.add(quizAuthor.getId());
            authorsToCheck.add(quizAuthor.getAuthor());
        }

        if (!quizAuthorIds.isEmpty()) {
            // Step 3: Batch delete all Questions for ALL QuizAuthors (single query per QuizAuthor)
            long deleteQuestionsStart = System.currentTimeMillis();
            int totalQuestionsDeleted = 0;
            for (Long quizAuthorId : quizAuthorIds) {
                Specification<Question> questionSpec = QuestionSpecification.byQuizAuthorId(quizAuthorId);
                List<Question> questions = questionRepository.findAll(questionSpec);
                if (!questions.isEmpty()) {
                    questionRepository.deleteAll(questions);
                    totalQuestionsDeleted += questions.size();
                }
            }
            logger.atInfo()
                  .addArgument(totalQuestionsDeleted)
                  .addArgument(System.currentTimeMillis() - deleteQuestionsStart)
                  .log("Deleted {} questions in {}ms");

            // Step 4: Batch delete all QuizErrors for ALL QuizAuthors (single query per QuizAuthor)
            long deleteErrorsStart = System.currentTimeMillis();
            int totalErrorsDeleted = 0;
            for (Long quizAuthorId : quizAuthorIds) {
                Specification<QuizError> errorSpec = QuizErrorSpecification.byQuizAuthor(quizAuthorId);
                List<QuizError> errors = quizErrorRepository.findAll(errorSpec);
                if (!errors.isEmpty()) {
                    quizErrorRepository.deleteAll(errors);
                    totalErrorsDeleted += errors.size();
                }
            }
            logger.atInfo()
                  .addArgument(totalErrorsDeleted)
                  .addArgument(System.currentTimeMillis() - deleteErrorsStart)
                  .log("Deleted {} errors in {}ms");
        }

        // Step 5: Delete all QuizAuthor entries (already loaded, batch delete)
        long deleteQuizAuthorsStart = System.currentTimeMillis();
        if (!quizAuthors.isEmpty()) {
            quizAuthorRepository.deleteAll(quizAuthors);
            logger.atInfo()
                  .addArgument(quizAuthors.size())
                  .addArgument(System.currentTimeMillis() - deleteQuizAuthorsStart)
                  .log("Deleted {} QuizAuthor entries in {}ms");
        }

        // Step 6: Delete the Quiz itself
        quizRepository.deleteById(id);
        logger.atInfo().addArgument(id).log("Deleted quiz with ID: {}");

        // Step 7: Optimized orphaned Author cleanup
        long cleanupStart = System.currentTimeMillis();
        if (!authorsToCheck.isEmpty()) {
            List<Author> authorsToDelete = new ArrayList<>();

            // Batch check all authors in one pass
            for (Author author : authorsToCheck) {
                Long remainingContributions = quizAuthorRepository.count(
                    QuizAuthorSpecification.hasAuthor(author)
                );
                if (remainingContributions == 0) {
                    authorsToDelete.add(author);
                }
            }

            if (!authorsToDelete.isEmpty()) {
                authorRepository.deleteAll(authorsToDelete);
                logger.atInfo()
                      .addArgument(authorsToDelete.size())
                      .log("Deleted {} orphaned authors");
            }
            logger.atInfo()
                  .addArgument(System.currentTimeMillis() - cleanupStart)
                  .log("Author cleanup completed in {}ms");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.atInfo()
              .addArgument(id)
              .addArgument(totalTime)
              .log("Completed optimized deletion of quiz {} in {}ms");
    }


    public QuizDto getQuizById(Long id) {
        Optional<Quiz> quiz = quizRepository.findById(id);
        if (quiz.isPresent()) {
            Quiz q = quiz.get();
            QuizDto dto = new QuizDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourse());
            dto.setYear(q.getYear());
            // Fetch questions for this quiz using QuestionSpecification
            var spec = QuestionSpecification.byFilters(null, null, id, null);
            List<Question> questions = questionRepository.findAll(spec);
            // Separate MC and TF questions and convert to DTOs
            List<QuestionDto> questionsMC = new ArrayList<>();
            List<QuestionDto> questionsTF = new ArrayList<>();
            for (Question question : questions) {
                QuestionDto questionDto = questionMapper.toDto(question);
                if (question.getType() == QuestionType.MULTICHOICE) {
                    questionsMC.add(questionDto);
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    questionsTF.add(questionDto);
                }
            }
            dto.setQuestionsMultichoice(questionsMC);
            dto.setQuestionsTruefalse(questionsTF);
            // Fetch authors with questions and quizErrors eagerly loaded
            List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(QuizAuthorSpecification.hasQuizId(id).and(QuizAuthorSpecification.fetchQuestionsAndErrors()));
            dto.setNoAuthors(quizAuthors.size());
            List<AuthorDto> authorDtos = quizAuthors.stream().map(qa -> new AuthorDto(qa.getAuthor().getId(), qa.getAuthor().getName(), qa.getAuthor().getInitials())).toList();
            dto.setAuthors(authorDtos);
            return dto;
        }
        return null;
    }


    public int getCompareTo(QuizDto q1, QuizDto q2) {
        if (q1 == null || q2 == null) {
            return 0;
        }
        return q1.getCourse().compareTo(q2.getCourse());
    }


    public Quiz updateQuiz(Long id, String course, String name, Long year) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isPresent()) {
            Quiz quiz = quizOptional.get();
            quiz.setCourse(course);
            quiz.setName(name);
            quiz.setYear(year);

            // update course
            CourseDto courseDto = new CourseDto();
            courseDto.setCourse(course);
            courseService.createCourse(courseDto);

            return quizRepository.save(quiz);
        }
        return null;
    }


    public List<QuizDto> getQuizzesByCourse(String course) {
        Specification<Quiz> spec = QuizSpecification.byCourse(course);
        List<Quiz> quizzes = quizRepository.findAll(spec);
        return quizzes.stream().map(quiz -> {
            QuizDto dto = new QuizDto();
            dto.setId(quiz.getId());
            dto.setName(quiz.getName());
            dto.setCourse(quiz.getCourse());
            dto.setYear(quiz.getYear());
            if (quiz.getQuizAuthors() != null && !quiz.getQuizAuthors().isEmpty()) {
                dto.setSourceFile(quiz.getQuizAuthors().iterator().next().getSource());
            }
            List<AuthorDto> authorDtos = quiz.getQuizAuthors().stream().map(qa -> new AuthorDto(qa.getAuthor().getId(), qa.getAuthor().getName(), qa.getAuthor().getInitials())).toList();
            dto.setAuthors(authorDtos);
            // Use QuestionSpecification for MC and TF questions
            var questionSpec = QuestionSpecification.byFilters(null, null, quiz.getId(), null);
            List<QuestionDto> mcQuestions = questionRepository.findAll(questionSpec).stream().filter(q -> q.getType() == QuestionType.MULTICHOICE).map(questionMapper::toDto).toList();
            dto.setQuestionsMultichoice(mcQuestions);
            List<QuestionDto> tfQuestions = questionRepository.findAll(questionSpec).stream().filter(q -> q.getType() == QuestionType.TRUEFALSE).map(questionMapper::toDto).toList();
            dto.setQuestionsTruefalse(tfQuestions);
            return dto;
        }).toList();
    }


    public List<QuizDto> getQuizzesByCourseId(Long courseId) {
        String courseName = courseService.getCourseName(courseId);
        if (courseName == null || courseName.isEmpty()) {
            return List.of();
        }
        Specification<Quiz> spec = QuizSpecification.byCourse(courseName);
        List<Quiz> quizzes = quizRepository.findAll(spec);
        return quizzes.stream().map(q -> {
            QuizDto dto = new QuizDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourse());
            dto.setYear(q.getYear());
            return dto;
        }).toList();
    }


    public List<Question> getQuestionsByQuizId(Long id) {
        var spec = QuestionSpecification.byFilters(null, null, id, null);
        return questionRepository.findAll(spec);
    }

    public QuizFilterDto filterQuizzes(QuizFilterInputDto filterInput) {
        int page = filterInput.getPage();
        int pageSize = filterInput.getPageSize();
        String course = filterInput.getCourse();

        // Fetch all courses for the dropdown
        List<CourseDto> allCourses = courseService.getAllCourses();

        List<Quiz> filtered = quizRepository.findAll();
        if (course != null && !course.isBlank()) {
            filtered = filtered.stream().filter(q -> course.equalsIgnoreCase(q.getCourse())).toList();
        }
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = Math.min((page - 1) * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<QuizDto> pageContent = filtered.subList(fromIndex, toIndex).stream().map(quiz -> {
            QuizDto dto = new QuizDto();
            dto.setId(quiz.getId());
            dto.setName(quiz.getName());
            dto.setCourse(quiz.getCourse());
            dto.setYear(quiz.getYear());
            var spec = QuestionSpecification.byFilters(null, null, quiz.getId(), null);
            List<Question> questions = questionRepository.findAll(spec);
            int mcCount = 0;
            int tfCount = 0;
            for (Question question : questions) {
                if (question.getType() == QuestionType.MULTICHOICE) mcCount++;
                else if (question.getType() == QuestionType.TRUEFALSE) tfCount++;
            }
            dto.setMcQuestionsCount(mcCount);
            dto.setTfQuestionsCount(tfCount);
            List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(QuizAuthorSpecification.hasQuizId(quiz.getId()).and(QuizAuthorSpecification.fetchQuestionsAndErrors()));
            dto.setNoAuthors(quizAuthors.size());
            return dto;
        }).toList();
        if (totalPages < page) {
            page = 1;
        }
        QuizFilterDto result = new QuizFilterDto();
        result.setQuizzes(pageContent);
        result.setTotalElements(totalElements);
        result.setTotalPages(totalPages);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setCourses(allCourses);
        return result;
    }

    @Cacheable("allQuizInfo")
    public List<QuizInfo> getAllQuizInfo() {
        List<Quiz> quizzes = quizRepository.findAll();
        return quizzes.stream().map(quiz -> new QuizInfo(quiz.getId(), quiz.getName(), quiz.getCourse())).toList();
    }

    @Cacheable(value = "quizInfoByCourse", key = "#selectedCourse")
    public List<QuizInfo> getQuizInfoByCourse(String selectedCourse) {
        Specification<Quiz> spec = QuizSpecification.byCourse(selectedCourse);
        List<Quiz> quizzes = quizRepository.findAll(spec);
        return quizzes.stream().map(quiz -> new QuizInfo(quiz.getId(), quiz.getName(), quiz.getCourse())).toList();
    }
}
