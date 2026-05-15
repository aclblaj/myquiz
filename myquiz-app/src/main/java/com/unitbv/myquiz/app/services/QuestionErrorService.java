package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterResponseDto;
import com.unitbv.myquiz.api.dto.CourseInfo;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionErrorService {

    private static final Logger log = LoggerFactory.getLogger(QuestionErrorService.class);

    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionRepository questionRepository;
    private final AuthorService authorService;
    private final QuestionBankService questionBankService;
    private final CourseService courseService;

    public QuestionErrorService(QuestionErrorRepository questionErrorRepository, QuestionRepository questionRepository, AuthorService authorService, @Lazy QuestionBankService questionBankService,
                                CourseService courseService) {
        this.questionErrorRepository = questionErrorRepository;
        this.questionRepository = questionRepository;
        this.authorService = authorService;
        this.questionBankService = questionBankService;
        this.courseService = courseService;
    }

    @Transactional
    public void addAuthorError(QuestionBankAuthor questionBankAuthor, Question question, String description) {
        if (questionBankAuthor == null) {
            throw new IllegalArgumentException("QuestionBankAuthor cannot be null");
        }
        if (question == null) {
            throw new IllegalArgumentException("Question cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }

        if (question.getQuestionBankAuthor() == null) {
            question.setQuestionBankAuthor(questionBankAuthor);
        }
        if (question.getType() == null) {
            question.setType(QuestionType.UNKNOWN);
        }
        if (question.getId() == null) {
            question = questionRepository.save(question);
        }

        QuestionError questionError = new QuestionError();
        questionError.setQuestion(question);
        questionError.setRowNumber(question.getCrtNo());
        questionError.setDescription(getDescriptionWithTitle(question, description));
        questionErrorRepository.save(questionError);
    }

    private String getDescriptionWithTitle(Question question, String description) {
        String title = (question != null && question.getTitle() != null) ? question.getTitle() : "";
        return title.isEmpty() ? description : description + " [" + title + "]";
    }

    public QuestionErrorFilterResponseDto getAuthorErrors(String selectedCourse, Long selectedCourseId, String selectedAuthor, Long selectedQuestionBankId, Integer page, Integer pageSize) {
        List<QuestionError> all = selectedQuestionBankId != null ? questionErrorRepository.findByQuestionQuestionBankAuthorQuestionBankId(selectedQuestionBankId) : questionErrorRepository.findAll();

        List<QuestionError> filtered = all.stream().filter(error -> matchCourse(error, selectedCourse)).filter(error -> matchAuthor(error, selectedAuthor)).sorted(
                Comparator.comparing(QuestionError::getRowNumber, Comparator.nullsLast(Integer::compareTo))).toList();

        int safePage = (page != null && page > 0) ? page : 1;
        int safePageSize = (pageSize != null && pageSize > 0) ? pageSize : ControllerSettings.PAGE_SIZE;
        int from = Math.min((safePage - 1) * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());

        List<QuestionErrorDto> dtos = filtered.subList(from, to).stream().map(this::mapToQuestionErrorDto).toList();
        Set<String> authorNames = filtered.stream().map(this::resolveAuthorName).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        QuestionErrorFilterResponseDto dto = new QuestionErrorFilterResponseDto();
        dto.setCourse(selectedCourse);
        dto.setSelectedCourseId(selectedCourseId);
        dto.setAuthorName(selectedAuthor);
        dto.setSelectedQuestionBankId(selectedQuestionBankId);
        dto.setQuestionErrors(dtos);
        dto.setAuthorNames(new ArrayList<>(authorNames));
        dto.setCourses(courseService.getAllCourses().stream().map(CourseInfo::from).toList());
        dto.setQuestionBanks(selectedCourse != null && !selectedCourse.isBlank() ? questionBankService.getQuestionBankInfoByCourse(selectedCourse) : List.of());
        dto.setQuestionErrorsByAuthor(groupByAuthor(dtos));
        dto.setPage(safePage);
        dto.setPageSize(safePageSize);
        dto.setTotalElements((long) filtered.size());
        dto.setTotalPages((int) Math.ceil((double) filtered.size() / safePageSize));
        return dto;
    }

    @Transactional(readOnly = true)
    public QuestionErrorFilterResponseDto filter(QuestionErrorFilterRequestDto filterInput) {
        if (filterInput == null) {
            throw new IllegalArgumentException("Filter input cannot be null");
        }
        String selectedCourse = filterInput.getSelectedCourse();
        Long selectedCourseId = filterInput.getSelectedCourseId();
        if (selectedCourseId != null) {
            selectedCourse = courseService.getCourseName(selectedCourseId);
        }
        return getAuthorErrors(selectedCourse, selectedCourseId, filterInput.getSelectedAuthor(), filterInput.getSelectedQuestionBankId(), filterInput.getPage(), filterInput.getPageSize());
    }

    public List<QuestionErrorDto> getErrorsByQuestionBankId(Long questionBankId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        return questionErrorRepository.findByQuestionQuestionBankAuthorQuestionBankId(questionBankId).stream().map(this::mapToQuestionErrorDto).sorted(
                Comparator.comparing(QuestionErrorDto::getRow, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    public int countErrorsByAuthorAndQuestionBank(Long authorId, Long questionBankId) {
        if (authorId == null || questionBankId == null) {
            throw new IllegalArgumentException("Author ID and QuestionBank ID cannot be null");
        }
        return (int) questionErrorRepository.countByQuestionQuestionBankAuthorAuthorIdAndQuestionQuestionBankAuthorQuestionBankId(authorId, questionBankId);
    }

    public List<QuestionErrorDto> getErrorsByQuestionBankAndAuthor(Long questionBankId, Long authorId) {
        if (questionBankId == null || authorId == null) {
            throw new IllegalArgumentException("QuestionBank ID and Author ID cannot be null");
        }
        return questionErrorRepository.findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(questionBankId, authorId).stream().map(this::mapToQuestionErrorDto).sorted(
                Comparator.comparing(QuestionErrorDto::getRow, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    public List<QuestionErrorDto> getErrorsForQuestionBankAuthor(Long questionBankAuthorId) {
        if (questionBankAuthorId == null) {
            throw new IllegalArgumentException("QuestionBankAuthor ID cannot be null");
        }
        return questionErrorRepository.findByQuestionQuestionBankAuthorId(questionBankAuthorId).stream().map(this::mapToQuestionErrorDto).sorted(
                Comparator.comparing(QuestionErrorDto::getRow, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    @Transactional
    public QuestionErrorDto resolveErrorById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Error ID cannot be null");
        }
        QuestionError error = questionErrorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Error not found with id: " + id));
        error.setStatus(ControllerSettings.ERROR_STATUS_RESOLVED);
        QuestionError saved = questionErrorRepository.save(error);
        return mapToQuestionErrorDto(saved);
    }

    @Transactional
    public boolean deleteErrorById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Error ID cannot be null");
        }
        if (!questionErrorRepository.existsById(id)) {
            return false;
        }
        questionErrorRepository.deleteById(id);
        return true;
    }

    public QuestionErrorDto getErrorById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Error ID cannot be null");
        }
        return questionErrorRepository.findById(id).map(this::mapToQuestionErrorDto).orElse(null);
    }

    public List<QuestionErrorDto> getAllErrors() {
        return questionErrorRepository.findAll().stream().map(this::mapToQuestionErrorDto).sorted(Comparator.comparing(QuestionErrorDto::getRow, Comparator.nullsLast(Integer::compareTo))).toList();
    }

    private boolean matchCourse(QuestionError error, String selectedCourse) {
        if (selectedCourse == null || selectedCourse.isBlank()) {
            return true;
        }

        String normalizedSelectedCourse = selectedCourse.trim();
        QuestionBankAuthor questionBankAuthor = error != null && error.getQuestion() != null
                ? error.getQuestion().getQuestionBankAuthor()
                : null;

        if (questionBankAuthor == null || questionBankAuthor.getQuestionBank() == null) {
            return false;
        }

        String questionBankCourse = questionBankAuthor.getQuestionBank().getCourseName();
        return questionBankCourse != null && questionBankCourse.trim().equalsIgnoreCase(normalizedSelectedCourse);
    }

    private boolean matchAuthor(QuestionError error, String selectedAuthor) {
        if (selectedAuthor == null || selectedAuthor.isBlank()) {
            return true;
        }
        String authorName = resolveAuthorName(error);
        return authorName != null && authorName.toLowerCase().contains(selectedAuthor.toLowerCase());
    }

    private String resolveAuthorName(QuestionError error) {
        QuestionBankAuthor qa = error.getQuestion() != null ? error.getQuestion().getQuestionBankAuthor() : null;
        if (qa == null || qa.getAuthor() == null) {
            return ControllerSettings.UNKNOWN;
        }
        return qa.getAuthor().getName();
    }

    private Map<String, List<QuestionErrorDto>> groupByAuthor(List<QuestionErrorDto> dtos) {
        Map<String, List<QuestionErrorDto>> grouped = new LinkedHashMap<>();
        for (QuestionErrorDto dto : dtos) {
            grouped.computeIfAbsent(dto.getAuthorName(), key -> new ArrayList<>()).add(dto);
        }
        return grouped;
    }

    private QuestionErrorDto mapToQuestionErrorDto(QuestionError error) {
        QuestionBankAuthor qa = error.getQuestion() != null ? error.getQuestion().getQuestionBankAuthor() : null;
        String authorName = ControllerSettings.UNKNOWN;
        Long authorId = null;
        String questionBankName = null;
        Long questionBankId = null;

        if (qa != null) {
            if (qa.getAuthor() != null) {
                authorName = qa.getAuthor().getName() != null ? qa.getAuthor().getName() : ControllerSettings.UNKNOWN;
                authorId = qa.getAuthor().getId();
            }
            if (qa.getQuestionBank() != null) {
                questionBankName = qa.getQuestionBank().getName();
                questionBankId = qa.getQuestionBank().getId();
            }
        }

        return QuestionErrorDto.builder()
                .id(error.getId())
                .description(error.getDescription())
                .row(error.getRowNumber())
                .authorName(authorName)
                .authorId(authorId)
                .questionBankName(questionBankName)
                .questionBankId(questionBankId)
                .questionId(error.getQuestion() != null ? error.getQuestion().getId() : null)
                .errorCode("QUESTION_ERROR")
                .status(error.getStatus() != null ? error.getStatus() : ControllerSettings.ERROR_STATUS_OPEN)
                .dateCreated(error.getCreatedAt() != null ? java.util.Date.from(error.getCreatedAt().toInstant()) : null)
                .questionType(error.getQuestion() != null && error.getQuestion().getType() != null ? error.getQuestion().getType().name() : null)
                .build();
    }
}

