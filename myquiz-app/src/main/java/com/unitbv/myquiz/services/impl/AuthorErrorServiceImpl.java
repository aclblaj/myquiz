package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquizapi.dto.AuthorErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unitbv.myquiz.services.impl.QuestionValidationServiceImpl.getDescriptionWithTitle;

@Service
public class AuthorErrorServiceImpl implements AuthorErrorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorErrorServiceImpl.class);
    private final QuizAuthorRepository quizAuthorRepository;
    AuthorErrorRepository authorErrorRepository;

    String sourceFile;

    AuthorService authorService;

    @Autowired
    public AuthorErrorServiceImpl(AuthorErrorRepository authorErrorRepository, QuizAuthorRepository quizAuthorRepository, AuthorService authorService) {
        this.authorErrorRepository = authorErrorRepository;
        this.quizAuthorRepository = quizAuthorRepository;
        this.authorService = authorService;
    }
    @Override
    public void addAuthorError(QuizAuthor quizAuthor, Question question, String description) {
        QuizError quizError = new QuizError();
        quizError.setDescription(getDescriptionWithTitle(question, description));
        quizError.setRowNumber(question.getCrtNo());
        quizError.setQuizAuthor(quizAuthor);
        quizAuthor.getQuizErrors().add(quizError);
        log.trace("Author error added: {} quizAuthor", quizError, quizError.getQuizAuthor().getAuthor().getName());
    }

    @Override
    public List<QuizError> getErrorsForAuthorName(String authorName) {
        return authorErrorRepository.findAllByQuizAuthor_Author_NameContainsIgnoreCase(authorName);
    }

    @Override
    public List<QuizError> getErrors(String course) {
        List<QuizError> quizErrors = new ArrayList<>();
        // find author quizzes by course
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAllWithQuestionsAndQuizErrorsByQuizCourseContainsIgnoreCase(course);
        // find errors for each author quiz
        for (QuizAuthor quizAuthor : quizAuthors) {
            log.info("QuizAuthor: author {}, course {}, no of questions {}, no of errors {}",
                quizAuthor.getAuthor() != null ? quizAuthor.getAuthor().getName() : "Unknown",
                quizAuthor.getQuiz() != null ? quizAuthor.getQuiz().getCourse() : "Unknown",
                quizAuthor.getQuestions() != null ? quizAuthor.getQuestions().size() : 0,
                quizAuthor.getQuizErrors() != null ? quizAuthor.getQuizErrors().size() : 0);
            // Changed from equalsIgnoreCase to contains to match courses like "25-BD-Q1" and "25-BD-Q1-v2"
            if (quizAuthor.getQuiz().getCourse().toLowerCase().contains(course.toLowerCase())) {
                Set<QuizError> quizErrorsForQA = quizAuthor.getQuizErrors();
                if (quizErrorsForQA.size() > 0) {
                    quizErrors.addAll(quizErrorsForQA);
                }
            }
        }
        return quizErrors;

//        return authorErrorRepository.findAllByOrderByQuizAuthor_Author_NameAsc();
//        return authorErrorRepository.findAllByQuizAuthor_Quiz_CourseContainsIgnoreCase(course);
    }

    @Override
    public void setSource(String filePath) {
        int pos = filePath.lastIndexOf(File.separator);
        String filename = filePath.substring(pos + 1);
        sourceFile = filename;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public void deleteAll() {
        authorErrorRepository.deleteAll();
    }

    @Override
    public void saveAllAuthorErrors(List<QuizError> quizErrors) {
        authorErrorRepository.saveAll(quizErrors);
    }

    @Override
    public List<QuizError> getErrorsForQuizAuthor(Long quizAuthorId) {
        return authorErrorRepository.findByQuizAuthorId(quizAuthorId);
    }

    public void getAuthorErrorsModel(Model model, String selectedCourse, String selectedAuthor) {
        List<String> courses = authorService.getCourseNames();
        if (selectedCourse == null || selectedCourse.isEmpty()) {
            selectedCourse = courses.size() > 0 ? courses.get(0) : "";
        }
        List<QuizError> quizErrors = getErrors(selectedCourse);
        if (selectedAuthor != null && !selectedAuthor.isEmpty()) {
            quizErrors = quizErrors.stream()
                .filter(e -> e.getQuizAuthor() != null
                    && e.getQuizAuthor().getAuthor() != null
                    && e.getQuizAuthor().getAuthor().getName() != null
                    && e.getQuizAuthor().getAuthor().getName().equalsIgnoreCase(selectedAuthor))
                .collect(Collectors.toList());
        }
        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        Set<String> authorNames = new HashSet<>();
        for (QuizError error : quizErrors) {
            String authorName = "Unknown";
            Long authorId = null;
            String quizName = null;
            Long questionId = null;
            if (error.getQuizAuthor() != null) {
                if (error.getQuizAuthor().getAuthor() != null) {
                    authorName = error.getQuizAuthor().getAuthor().getName() != null ? error.getQuizAuthor().getAuthor().getName() : "Unknown";
                    authorId = error.getQuizAuthor().getAuthor().getId();
                }
                if (error.getQuizAuthor().getQuiz() != null) {
                    quizName = error.getQuizAuthor().getQuiz().getName();
                }
                if (error.getRowNumber() != null && error.getQuizAuthor().getQuestions() != null) {
                    Question question = error.getQuizAuthor().getQuestions().stream()
                        .filter(q -> error.getRowNumber().equals(q.getCrtNo()))
                        .findFirst()
                        .orElse(null);
                    if (question != null) {
                        questionId = question.getId();
                    }
                }
            }
            AuthorErrorDto dto = new AuthorErrorDto(
                error.getDescription(),
                error.getRowNumber(),
                authorName,
                authorId
            );
            if (quizName != null) {
                dto.setQuizName(quizName);
            }
            if (questionId != null) {
                dto.setQuestionId(questionId);
            }
            if (dto.getAuthorName() != null) {
                authorNames.add(dto.getAuthorName());
            }
            authorErrorDtos.add(dto);
        }

        Map<String, List<AuthorErrorDto>> errorsByAuthor = new HashMap<>();
        for (AuthorErrorDto error : authorErrorDtos) {
            errorsByAuthor.computeIfAbsent(error.getAuthorName(), k -> new ArrayList<>()).add(error);
        }

        Map<String, List<AuthorErrorDto>> sortedErrorsByAuthor = errorsByAuthor.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    getGetValue(),
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                )
            );

        model.addAttribute("errorsByAuthor", sortedErrorsByAuthor);
        model.addAttribute("errors", authorErrorDtos);
        model.addAttribute("authors", new ArrayList<>(authorNames));
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("selectedAuthor", selectedAuthor);
        model.addAttribute("route", "errors/");
    }

    private QuizError[] getErrors(String selectedCourse, String author) {
        List<QuizError> quizErrors = getErrors(selectedCourse);
        if (author != null && !author.isEmpty()) {
            quizErrors = quizErrors.stream()
                .filter(e -> e.getQuizAuthor() != null
                    && e.getQuizAuthor().getAuthor() != null
                    && e.getQuizAuthor().getAuthor().getName() != null
                    && e.getQuizAuthor().getAuthor().getName().equalsIgnoreCase(author))
                .collect(Collectors.toList());
        }
        return quizErrors.toArray(new QuizError[0]);
    }

    private Function<Map.Entry<String, List<AuthorErrorDto>>, List<AuthorErrorDto>> getGetValue() {
        //sort the list of values
        return e -> e.getValue().stream()
                     .sorted((o1, o2) -> getComparedTo(o1, o2))
                     .collect(Collectors.toList());
    }

    public int getComparedTo(AuthorErrorDto o1, AuthorErrorDto o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        Integer o1Row = (o1 == null) ? null : o1.getRow();
        Integer o2Row = (o2 == null) ? null : o2.getRow();
        return Optional.ofNullable(o1Row).orElse(0).compareTo(Optional.ofNullable(o2Row).orElse(0));
    }

    public List<AuthorErrorDto> sortAuthorErrorsByRow(List<AuthorErrorDto> authorErrorDtos) {
        List<AuthorErrorDto> sortedAuthorErrorDtos = new ArrayList<>();
        if (authorErrorDtos == null) {
            return sortedAuthorErrorDtos;
        }

        sortedAuthorErrorDtos.addAll(authorErrorDtos);
        sortedAuthorErrorDtos.sort((o1, o2) -> getComparedTo(o1, o2));

        return sortedAuthorErrorDtos;
    }

    @Override
    public List<AuthorErrorDto> getErrorsByQuizId(Long quizId) {
        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        // Find all quiz authors for this quiz, with questions and quizErrors eagerly loaded
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findWithQuestionsAndQuizErrorsByQuizId(quizId);
        for (QuizAuthor quizAuthor : quizAuthors) {
            Set<QuizError> quizErrors = quizAuthor.getQuizErrors();
            for (QuizError error : quizErrors) {
                AuthorErrorDto dto = new AuthorErrorDto(
                    error.getId(),
                    error.getDescription(),
                    error.getRowNumber(),
                    quizAuthor.getAuthor() != null ? quizAuthor.getAuthor().getName() : "Unknown",
                    quizAuthor.getAuthor() != null ? quizAuthor.getAuthor().getId() : null
                );
                // Find the question ID by row number
                if (error.getRowNumber() != null) {
                    Question question = quizAuthor.getQuestions().stream()
                        .filter(q -> error.getRowNumber().equals(q.getCrtNo()))
                        .findFirst()
                        .orElse(null);
                    if (question != null) {
                        dto.setQuestionId(question.getId());
                    }
                }
                authorErrorDtos.add(dto);
            }
        }
        return sortAuthorErrorsByRow(authorErrorDtos);
    }

    @Override
    public List<String> getAvailableCourses() {
        // TODO: Replace with actual course retrieval logic if needed
        return java.util.Arrays.asList("Course1", "Course2", "Course3");
    }

}
