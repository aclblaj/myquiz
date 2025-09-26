package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.AuthorErrorDto;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unitbv.myquiz.services.QuestionServiceImpl.getDescriptionWithTitle;

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
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAllByQuiz_CourseContainsIgnoreCase(course);
        // find errors for each author quiz
        for (QuizAuthor quizAuthor : quizAuthors) {
            log.atInfo().log("QuizAuthor: author {}, course {}, no of questions {}, no of errors {}",
                             quizAuthor.getAuthor().getName(), quizAuthor.getQuiz().getCourse(), quizAuthor.getQuestions().size(),
                             quizAuthor.getQuizErrors().size());
            if (quizAuthor.getQuiz().getCourse().equalsIgnoreCase(course)) {
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

    public void getAuthorErrorsModel(Model model, String selectedCourse) {

        List<String> courses = authorService.getCourseNames();
        if ( selectedCourse == null) {
            selectedCourse = courses.size() > 0 ? courses.get(0) : "";
        }

        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        for (QuizError error : getErrors(selectedCourse)) {
            authorErrorDtos.add(new AuthorErrorDto(error));
        }

        Map<String, List<AuthorErrorDto>> errorsByAuthor = new HashMap<>();
        for (AuthorErrorDto error : authorErrorDtos) {
            errorsByAuthor.computeIfAbsent(error.getAuthorName(), k -> new ArrayList<>()).add(error);
        }

        // sort map by key
        Map<String, List<AuthorErrorDto>> sortedErrorsByAuthor = errorsByAuthor.entrySet().stream()
                                                                               .sorted(Map.Entry.comparingByKey())
                                                                               .collect(
                                                                                       Collectors.toMap(
                                                                                               Map.Entry::getKey,
                                                                                               getGetValue(),
                                                                                               (e1, e2) -> e1,
                                                                                               LinkedHashMap::new // Preserve the order
                                                                                       )
                                                                               );


        model.addAttribute("errorsByAuthor", sortedErrorsByAuthor);
        model.addAttribute("errors", authorErrorDtos);

        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("route", "errors/");
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

}
