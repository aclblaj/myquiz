package com.unitbv.myquiz.app.entities;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * Stores a duplicate relationship between two questions.
 * The service persists the pair in canonical order to avoid duplicate rows.
 */
@Entity
@Table(name = "question_duplicate", uniqueConstraints = {@UniqueConstraint(name = "uk_question_duplicate_pair", columnNames = {"question_id", "duplicate_question_id"})}, indexes = {@Index(name = "idx_question_duplicate_question_id", columnList = "question_id"), @Index(name = "idx_question_duplicate_duplicate_question_id", columnList = "duplicate_question_id")})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"question", "duplicateQuestion"})
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDuplicate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_duplicate_gen")
    @SequenceGenerator(name = "question_duplicate_gen", sequenceName = "question_duplicate_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_question_id", nullable = false)
    private Question duplicateQuestion;


    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "status", length = 20)
    private String status = ControllerSettings.DUPLICATE_STATUS_OPEN;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

