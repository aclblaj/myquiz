package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.specifications.QuestionBankAuthorSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionBankAuthorService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionBankAuthorService.class);

    private final QuestionBankAuthorRepository questionBankAuthorRepository;

    @Autowired
    public QuestionBankAuthorService(QuestionBankAuthorRepository questionBankAuthorRepository) {
        this.questionBankAuthorRepository = questionBankAuthorRepository;
    }

    @Transactional
    public void deleteAll() {
        questionBankAuthorRepository.deleteAll();
    }

    public List<QuestionBankAuthor> getQuestionBankAuthorsForAuthorName(String authorName) {
        // Input validation
        if (authorName == null || authorName.isBlank()) {
            logger.atWarn().log("Author name is null or empty");
            return List.of();
        }

        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasAuthorName(authorName));
    }

    public List<QuestionBankAuthor> getQuestionBankAuthorsForAuthorId(Long authorId) {
        // Input validation
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }

        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasAuthorId(authorId).and(QuestionBankAuthorSpecification.fetchQuestions()));
    }

    @Transactional
    public void deleteQuestionBankAuthorsByIds(List<Long> idsQA) {
        // Input validation
        if (idsQA == null || idsQA.isEmpty()) {
            logger.atWarn().log("No IDs provided for deletion");
            return;
        }

        questionBankAuthorRepository.deleteAllById(idsQA);
    }

    public List<QuestionBankAuthor> getQuestionBankAuthorsWithQuestionsByQuestionBankId(Long questionBankId) {
        // Input validation
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }

        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId).and(QuestionBankAuthorSpecification.fetchQuestions()));
    }

    public Optional<QuestionBankAuthor> getQuestionBankAuthorByQuestionBankIdAndAuthorId(Long questionBankId, Long authorId) {
        // Input validation
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }

        return questionBankAuthorRepository.findOne(QuestionBankAuthorSpecification.byQuestionBankAndAuthor(questionBankId, authorId));
    }

    public Set<String> getAuthorNames(Long questionBankId) {
        // Input validation
        if (questionBankId == null) {
            logger.atWarn().log("QuestionBank ID is null");
            return Set.of(ControllerSettings.UNKNOWN);
        }

        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
                QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId).and(QuestionBankAuthorSpecification.fetchAuthor()));

        if (questionBankAuthors.isEmpty()) {
            return Set.of(ControllerSettings.UNKNOWN);
        }

        return questionBankAuthors.stream().map(qa -> qa.getAuthor() != null ? qa.getAuthor().getName() : ControllerSettings.UNKNOWN).collect(Collectors.toSet());
    }

    public Set<AuthorDto> getAuthorDtos(Long questionBankId) {
        // Input validation
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }

        return questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId).and(QuestionBankAuthorSpecification.fetchAuthor())).stream().map(qa -> {
            AuthorDto dto = new AuthorDto();
            if (qa.getAuthor() != null) {
                dto.setId(qa.getAuthor().getId());
                dto.setName(qa.getAuthor().getName());
            } else {
                dto.setName(ControllerSettings.UNKNOWN);
            }
            return dto;
        }).collect(Collectors.toSet());
    }

    public List<AuthorInfo> getAuthorDtosByCourse(String course) {
        // Input validation
        if (course == null || course.isBlank()) {
            logger.atWarn().log("Course is null or empty");
            return List.of();
        }

        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasCourse(course).and(QuestionBankAuthorSpecification.fetchAuthor()));

        // Use a Map to collect distinct authors based on author ID, then convert to sorted List
        return questionBankAuthors.stream().filter(qa -> qa.getAuthor() != null) // Filter out null authors
                                  .collect(Collectors.toMap(
                                          qa -> qa.getAuthor().getId(), // Key: author ID (ensures uniqueness)
                                          qa -> {
                                              AuthorInfo dto = new AuthorInfo();
                                              dto.setId(qa.getAuthor().getId());
                                              dto.setName(qa.getAuthor().getName());
                                              return dto;
                                          }, (existing, replacement) -> existing // Keep first occurrence if duplicate
                                  )).values().stream().sorted((a1, a2) -> {
                    String name1 = a1.getName() != null ? a1.getName() : "";
                    String name2 = a2.getName() != null ? a2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                }).toList();
    }

    /**
     * Get all distinct authors who contributed questions to a specific questionBank.
     * Used for duplicate validation after bulk uploads.
     *
     * @param questionBankId The questionBank ID
     * @return ArrayList of Author entities
     */
    public ArrayList<Author> getAuthorsForQuestionBank(Long questionBankId) {
        // Input validation
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }

        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
                QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId).and(QuestionBankAuthorSpecification.fetchAuthor()));

        // Use LinkedHashSet for O(n) performance and maintain insertion order
        LinkedHashSet<Author> authorsSet = new LinkedHashSet<>();
        for (QuestionBankAuthor qa : questionBankAuthors) {
            if (qa.getAuthor() != null) {
                authorsSet.add(qa.getAuthor());
            }
        }

        return new ArrayList<>(authorsSet);
    }
}
