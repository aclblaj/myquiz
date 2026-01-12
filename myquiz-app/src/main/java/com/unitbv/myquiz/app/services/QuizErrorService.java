package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorErrorDto;
import com.unitbv.myquiz.api.dto.AuthorErrorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorErrorFilterInputDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuizErrorRepository;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuizErrorSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizErrorService {

    private static final Logger log = LoggerFactory.getLogger(QuizErrorService.class);
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuizErrorRepository quizErrorRepository;
    private final AuthorService authorService;
    private final QuizAuthorService quizAuthorService;

    public QuizErrorService(QuizErrorRepository quizErrorRepository, QuizAuthorRepository quizAuthorRepository,
                            AuthorService authorService, QuizAuthorService quizAuthorService) {
        this.quizErrorRepository = quizErrorRepository;
        this.quizAuthorRepository = quizAuthorRepository;
        this.authorService = authorService;
        this.quizAuthorService = quizAuthorService;
    }

    public void addAuthorError(QuizAuthor quizAuthor, Question question, String description) {
        QuizError quizError = new QuizError();
        quizError.setDescription(getDescriptionWithTitle(question, description));
        if (question != null) {
            quizError.setRowNumber(question.getCrtNo());
        }
        quizError.setQuizAuthor(quizAuthor);
        quizAuthor.getQuizErrors().add(quizError);
        quizErrorRepository.save(quizError);
        log.trace("Author error added: {} for quizAuthor {}", quizError, quizError.getQuizAuthor().getAuthor().getName());
    }

    // Moved from QuestionValidationService
    private String getDescriptionWithTitle(Question question, String description) {
        String title = (question != null && question.getTitle() != null) ? question.getTitle() : "";
        return title.isEmpty() ? description : description + " [" + title + "]";
    }


    public AuthorErrorFilterDto getAuthorErrors(String selectedCourse, String selectedAuthor, Integer page, Integer pageSize) {
        log.atInfo().log("Getting author errors model for course: {} and author: {}, page: {}, pageSize: {}",
                         selectedCourse, selectedAuthor, page, pageSize);
        List<String> courses = authorService.getCourseNames();
        String courseFilter;
        if (selectedCourse == null || selectedCourse.isEmpty()) {
            courseFilter = !courses.isEmpty() ? courses.get(0) : "";
        } else {
            courseFilter = selectedCourse;
        }
        String coursePattern = (courseFilter != null && !courseFilter.isBlank()) ? ("%" + courseFilter.toLowerCase() + "%") : null;

        String authorFilter = (selectedAuthor != null && !selectedAuthor.isEmpty()) ? selectedAuthor : null;
        String authorPattern = (authorFilter != null && !authorFilter.isBlank()) ? ("%" + authorFilter.toLowerCase() + "%") : null;

        Pageable pageable = getPageable(page, pageSize);

        // Use specification with eager fetching to avoid N+1 queries
        Specification<QuizError> spec = QuizErrorSpecification.byCourseAndAuthor(coursePattern, authorPattern)
                .and(QuizErrorSpecification.fetchQuizAuthorWithDetails());
        Page<QuizError> quizErrorPage = quizErrorRepository.findAll(spec, pageable);

        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        Set<String> authorNames = new HashSet<>();
        Long quizId = null;
        for (QuizError error : quizErrorPage.getContent()) {
            String authorName = ControllerSettings.UNKNOWN;
            Long authorId = null;
            String quizName = null;

            Long questionId = null;
            if (error.getQuizAuthor() != null) {
                if (error.getQuizAuthor().getAuthor() != null) {
                    authorName = error.getQuizAuthor().getAuthor().getName() != null ? error.getQuizAuthor().getAuthor().getName() : ControllerSettings.UNKNOWN;
                    authorId = error.getQuizAuthor().getAuthor().getId();
                }
                if (error.getQuizAuthor().getQuiz() != null) {
                    quizName = error.getQuizAuthor().getQuiz().getName();
                    quizId = error.getQuizAuthor().getQuiz().getId();
                }
                if (error.getRowNumber() != null && error.getQuizAuthor().getQuestions() != null) {
                    Question question = error.getQuizAuthor().getQuestions().stream().filter(q -> error.getRowNumber().equals(q.getCrtNo())).findFirst().orElse(null);
                    if (question != null) {
                        questionId = question.getId();
                    }
                }
            }

            AuthorErrorDto dto = new AuthorErrorDto(error.getDescription(), error.getRowNumber(), authorName, authorId);
            dto.setId(error.getId());
            if (quizName != null) { dto.setQuizName(quizName); }
            if (quizId != null) { dto.setQuizId(quizId); }
            if (questionId != null) { dto.setQuestionId(questionId); }
            if (dto.getAuthorName() != null) {
                authorNames.add(dto.getAuthorName());
            }
            authorErrorDtos.add(dto);
        }

        if (selectedAuthor == null || selectedAuthor.isEmpty()) {
            authorNames = quizAuthorService.getAuthorNames(quizId);
        }

        Map<String, List<AuthorErrorDto>> sortedErrorsByAuthor = getSortedErrorsByAuthor(authorErrorDtos);

        AuthorErrorFilterDto dto = new AuthorErrorFilterDto();
        dto.setCourse(courseFilter);
        dto.setAuthorName(selectedAuthor);
        dto.setAuthorErrors(authorErrorDtos);
        dto.setAuthorNames(new ArrayList<>(authorNames));
        dto.setCourses(courses);
        dto.setAuthorErrorsByAuthor(sortedErrorsByAuthor);
        dto.setPage(quizErrorPage.getNumber() + 1);
        dto.setPageSize(quizErrorPage.getSize());
        dto.setTotalElements(quizErrorPage.getTotalElements());
        dto.setTotalPages(quizErrorPage.getTotalPages());
        return dto;
    }

    private Map<String, List<AuthorErrorDto>> getSortedErrorsByAuthor(List<AuthorErrorDto> authorErrorDtos) {
        Map<String, List<AuthorErrorDto>> errorsByAuthor = new HashMap<>();
        for (AuthorErrorDto error : authorErrorDtos) {
            errorsByAuthor.computeIfAbsent(error.getAuthorName(), k -> new ArrayList<>()).add(error);
        }
        return errorsByAuthor.entrySet().stream()
                             .sorted(Map.Entry.comparingByKey())
                             .collect(
                                     Collectors.toMap(
                                             Map.Entry::getKey,
                                             e -> e.getValue().stream()
                                                   .sorted(this::getComparedTo).toList(),
                                             (e1, e2) -> e1,
                                             LinkedHashMap::new)
                             );
    }


    private static Pageable getPageable(Integer page, Integer pageSize) {
        int p = (page != null && page > 0) ? page - 1 : 0; // zero-based for Pageable
        int ps = (pageSize != null && pageSize > 0) ? pageSize : ControllerSettings.PAGE_SIZE;
        return PageRequest.of(p, ps,
                              Sort.by(Sort.Order.asc("quizAuthor.author.name"),
                                      Sort.Order.asc("rowNumber"))
        );
    }

    public int getComparedTo(AuthorErrorDto o1, AuthorErrorDto o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        Integer o1Row = (o1 == null) ? null : o1.getRow();
        Integer o2Row = (o2 == null) ? null : o2.getRow();
        return Optional.ofNullable(o1Row)
                       .orElse(0)
                       .compareTo(Optional.ofNullable(o2Row).orElse(0));
    }

    public List<AuthorErrorDto> sortAuthorErrorsByRow(List<AuthorErrorDto> authorErrorDtos) {
        List<AuthorErrorDto> sortedAuthorErrorDtos = new ArrayList<>();
        if (authorErrorDtos == null) {
            return sortedAuthorErrorDtos;
        }
        sortedAuthorErrorDtos.addAll(authorErrorDtos);
        sortedAuthorErrorDtos.sort(this::getComparedTo);
        return sortedAuthorErrorDtos;
    }


    public List<AuthorErrorDto> getErrorsByQuizId(Long quizId) {
        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        // Find all quiz authors for this quiz, with questions and quizErrors eagerly loaded
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.fetchQuestionsAndErrors())
        );
        for (QuizAuthor quizAuthor : quizAuthors) {
            Set<QuizError> quizErrors = quizAuthor.getQuizErrors();
            for (QuizError error : quizErrors) {
                AuthorErrorDto dto = new AuthorErrorDto(
                        error.getId(), error.getDescription(), error.getRowNumber(), quizAuthor.getAuthor() != null ? quizAuthor.getAuthor().getName() : ControllerSettings.UNKNOWN,
                        quizAuthor.getAuthor() != null ? quizAuthor.getAuthor().getId() : null
                );
                // Find the question ID by row number
                if (error.getRowNumber() != null) {
                    Question question = quizAuthor.getQuestions().stream().filter(q -> error.getRowNumber().equals(q.getCrtNo())).findFirst().orElse(null);
                    if (question != null) {
                        dto.setQuestionId(question.getId());
                    }
                }
                authorErrorDtos.add(dto);
            }
        }
        return sortAuthorErrorsByRow(authorErrorDtos);
    }

    public int countErrorsByAuthorAndQuiz(Long authorId, Long id) {
        log.info("[AuthorErrorServiceImpl] Counting errors for authorId={}, quizId={}", authorId, id);
        Specification<QuizError> spec = QuizErrorSpecification.byAuthorAndQuiz(authorId, id);
        long count = quizErrorRepository.count(spec);
        log.info("[AuthorErrorServiceImpl] Found {} errors for authorId={}, quizId={}", count, authorId, id);
        return (int) count;
    }

    @Transactional(readOnly = true)
    public AuthorErrorFilterDto filter(AuthorErrorFilterInputDto filterInput) {
        String selectedCourse = filterInput.getSelectedCourse();
        String selectedAuthor = filterInput.getSelectedAuthor();
        Integer page = filterInput.getPage();
        Integer pageSize = filterInput.getPageSize();
        return getAuthorErrors(selectedCourse, selectedAuthor, page, pageSize);
    }

    public List<AuthorErrorDto> getErrorsByQuizAndAuthor(Long quizId, Long authorId) {
        List<AuthorErrorDto> result = new ArrayList<>();
        // Use Specification with eager fetching to avoid LazyInitializationException
        Optional<QuizAuthor> optQuizAuthor = quizAuthorRepository.findOne(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.hasAuthorId(authorId))
                .and(QuizAuthorSpecification.fetchQuizErrors())
        );
        if (optQuizAuthor.isPresent()) {
            QuizAuthor qa = optQuizAuthor.get();
            Set<QuizError> errors = qa.getQuizErrors();
            if (errors != null) {
                for (QuizError error : errors) {
                    AuthorErrorDto dto = new AuthorErrorDto();
                    dto.setMessage(error.getDescription());
                    dto.setRow(error.getRowNumber());
                    dto.setAuthorName(qa.getAuthor() != null ? qa.getAuthor().getName() : null);
                    dto.setAuthorId(qa.getAuthor() != null ? qa.getAuthor().getId() : null);
                    result.add(dto);
                }
            }
        }
        return result;
    }

    public List<AuthorErrorDto> getErrorsForQuizAuthor(Long quizAuthorId) {
        List<AuthorErrorDto> result = new ArrayList<>();
        Optional<QuizAuthor> optQuizAuthor = quizAuthorRepository.findById(quizAuthorId);
        if (optQuizAuthor.isPresent()) {
            QuizAuthor qa = optQuizAuthor.get();
            Set<QuizError> errors = qa.getQuizErrors();
            if (errors != null) {
                for (QuizError error : errors) {
                    AuthorErrorDto dto = new AuthorErrorDto();
                    dto.setMessage(error.getDescription());
                    dto.setRow(error.getRowNumber());
                    dto.setAuthorName(qa.getAuthor() != null ? qa.getAuthor().getName() : null);
                    dto.setAuthorId(qa.getAuthor() != null ? qa.getAuthor().getId() : null);
                    result.add(dto);
                }
            }
        }
        return result;
    }

    public void saveAllQuizErrors(List<QuizError> duplicateErrors) {
        quizErrorRepository.saveAll(duplicateErrors);
    }
}
