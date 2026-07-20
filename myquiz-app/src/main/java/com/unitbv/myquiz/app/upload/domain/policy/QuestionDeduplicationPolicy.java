package com.unitbv.myquiz.app.upload.domain.policy;

import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import org.springframework.stereotype.Component;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QuestionDeduplicationPolicy {
    private final QuestionRepository questionRepository;

    public QuestionDeduplicationPolicy(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Set<String> buildCourseQuestionCache(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return new HashSet<>();
        }
        Specification<Question> specification = QuestionSpecification.byCourse(courseName);
        List<Question> existingQuestions = questionRepository.findAll(specification);

        Set<String> cache = new HashSet<>();
        for (Question question : existingQuestions) {
            String key = buildQuestionKey(question.getTitle(), question.getText());
            if (key != null) {
                cache.add(key);
            }
        }
        return cache;
    }

    public String buildQuestionKey(String title, String text) {
        String normalizedTitle = normalizeForComparison(title);
        String normalizedText = normalizeForComparison(text);
        if (normalizedTitle == null || normalizedText == null) {
            return null;
        }
        return normalizedTitle + "||" + normalizedText;
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}

