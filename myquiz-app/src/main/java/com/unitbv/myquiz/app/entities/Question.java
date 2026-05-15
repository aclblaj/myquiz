package com.unitbv.myquiz.app.entities;

import com.unitbv.myquiz.api.types.QuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a question_bank question.
 * Supports multiple question types including multiple choice and true/false questions.
 * Questions belong to a QuestionBankAuthor and can have associated errors.
 */
@Entity
@Table(name = "question", indexes = {@Index(name = "idx_qbi_question_bank_author_id", columnList = "question_bank_author_id"), @Index(name = "idx_qbi_type", columnList = "type")})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"questionBankAuthor", "questionErrors", "duplicateLinks", "duplicateOfLinks", "answersReference"})
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_gen")
    @SequenceGenerator(name = "question_gen", sequenceName = "question_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "crt_no")
    private int crtNo;

    @Column(name = "chapter", length = 100)
    private String chapter;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "text", length = 2048)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private QuestionType type;

    // Multiple choice responses and weights
    @Column(name = "weight_response1")
    private Double weightResponse1;

    @Column(name = "response1", length = 2048)
    private String response1;

    @Column(name = "weight_response2")
    private Double weightResponse2;

    @Column(name = "response2", length = 2048)
    private String response2;

    @Column(name = "weight_response3")
    private Double weightResponse3;

    @Column(name = "response3", length = 2048)
    private String response3;

    @Column(name = "weight_response4")
    private Double weightResponse4;

    @Column(name = "response4", length = 2048)
    private String response4;

    // True/false weights
    @Column(name = "weight_true")
    private Double weightTrue;

    @Column(name = "weight_false")
    private Double weightFalse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_author_id")
    private QuestionBankAuthor questionBankAuthor;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionError> questionErrors = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionDuplicate> duplicateLinks = new ArrayList<>();

    @OneToMany(mappedBy = "duplicateQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionDuplicate> duplicateOfLinks = new ArrayList<>();

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AnswersReference answersReference;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Question() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Utility method for row number
    public int getRow() {
        return crtNo;
    }

    // Helper methods for managing bidirectional relationship
    public void addQuestionError(QuestionError questionError) {
        questionErrors.add(questionError);
        questionError.setQuestion(this);
    }

    public void removeQuestionError(QuestionError questionError) {
        questionErrors.remove(questionError);
        questionError.setQuestion(null);
    }

    public String getAnswerReferenceText() {
        return answersReference != null ? answersReference.getReferenceText() : null;
    }

    public void setAnswerReferenceText(String referenceText) {
        if (referenceText == null || referenceText.isBlank()) {
            if (answersReference != null) {
                answersReference.setQuestion(null);
            }
            answersReference = null;
            return;
        }

        if (answersReference == null) {
            answersReference = new AnswersReference();
            answersReference.setQuestion(this);
        }
        answersReference.setReferenceText(referenceText);
    }

    @Transient
    public int getDuplicateCount() {
        return duplicateLinks.size() + duplicateOfLinks.size();
    }
}
