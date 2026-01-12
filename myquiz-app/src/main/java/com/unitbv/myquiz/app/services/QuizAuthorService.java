package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizAuthorService {
    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    public QuizAuthorService(QuizAuthorRepository quizAuthorRepository) {
        this.quizAuthorRepository = quizAuthorRepository;
    }

    public void deleteAll() {
        quizAuthorRepository.deleteAll();
    }

    public List<QuizAuthor> getQuizAuthorsForAuthorName(String authorName) {
        return quizAuthorRepository.findAll(QuizAuthorSpecification.hasAuthorName(authorName));
    }

    public List<QuizAuthor> getQuizAuthorsForAuthorId(Long authorId) {
        return quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasAuthorId(authorId)
                .and(QuizAuthorSpecification.fetchQuestions())
        );
    }

    public void deleteQuizAuthorsByIds(List<Long> idsQA) {
        quizAuthorRepository.deleteAllById(idsQA);
    }

    public List<QuizAuthor> getQuizAuthorsWithQuestionsAndErrorsByQuizId(Long quizId) {
        return quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.fetchQuestionsAndErrors())
        );
    }

    public Optional<QuizAuthor> getQuizAuthorByQuizIdAndAuthorId(Long quizId, Long authorId) {
        return quizAuthorRepository.findOne(
            QuizAuthorSpecification.byQuizAndAuthor(quizId, authorId)
        );
    }

    public Set<String> getAuthorNames(Long quizId) {
        Set<String> authorNames;
        Optional<QuizAuthor> first = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.fetchAuthor())
        ).stream().findFirst();

        if (first.isEmpty()) {
            return Set.of(ControllerSettings.UNKNOWN);
        }
        authorNames = first.get().getQuiz().getQuizAuthors().stream()
                           .map(qa -> qa.getAuthor() != null ? qa.getAuthor().getName() : ControllerSettings.UNKNOWN)
                           .collect(Collectors.toSet());
        return authorNames;
    }

    public Set<AuthorDto> getAuthorDtos(Long quizId) {
        Set<AuthorDto> authorDtos;
        authorDtos = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.fetchAuthor())
        ).stream()
                .map(qa -> {
                    AuthorDto dto = new AuthorDto();
                    if (qa.getAuthor() != null) {
                        dto.setId(qa.getAuthor().getId());
                        dto.setName(qa.getAuthor().getName());
                    } else {
                        dto.setName(ControllerSettings.UNKNOWN);
                    }
                    return dto;
                })
                .collect(Collectors.toSet());
        return authorDtos;
    }

    public List<AuthorInfo> getAuthorDtosByCourse(String course) {
        // Fetch QuizAuthors for the course with author eagerly loaded
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasCourse(course)
                .and(QuizAuthorSpecification.fetchAuthor())
        );

        // Use a Map to collect distinct authors based on author ID, then convert to sorted List
        return quizAuthors.stream()
                .filter(qa -> qa.getAuthor() != null) // Filter out null authors
                .collect(Collectors.toMap(
                    qa -> qa.getAuthor().getId(), // Key: author ID (ensures uniqueness)
                    qa -> {
                        AuthorInfo dto = new AuthorInfo();
                        dto.setId(qa.getAuthor().getId());
                        dto.setName(qa.getAuthor().getName());
                        return dto;
                    },
                    (existing, replacement) -> existing // Keep first occurrence if duplicate
                ))
                .values()
                .stream()
                .sorted((a1, a2) -> {
                    String name1 = a1.getName() != null ? a1.getName() : "";
                    String name2 = a2.getName() != null ? a2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                })
                .toList();
    }

    /**
     * Get all distinct authors who contributed questions to a specific quiz.
     * Used for duplicate validation after bulk uploads.
     *
     * @param quizId The quiz ID
     * @return ArrayList of Author entities
     */
    public ArrayList<Author> getAuthorsForQuiz(Long quizId) {
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAll(
            QuizAuthorSpecification.hasQuizId(quizId)
                .and(QuizAuthorSpecification.fetchAuthor())
        );
        ArrayList<Author> authors = new ArrayList<>();

        for (QuizAuthor qa : quizAuthors) {
            if (qa.getAuthor() != null && !authors.contains(qa.getAuthor())) {
                authors.add(qa.getAuthor());
            }
        }

        return authors;
    }
}
