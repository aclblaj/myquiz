package com.unitbv.myquiz.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
 * Optional 1:1 reference text attached to a question.
 */
@Entity
@Table(name = "answers_reference", uniqueConstraints = {
        @UniqueConstraint(name = "uk_answers_reference_question_id", columnNames = "question_id")
}, indexes = {
        @Index(name = "idx_answers_reference_question_id", columnList = "question_id")
})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"question"})
@NoArgsConstructor
@AllArgsConstructor
public class AnswersReference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "answers_reference_gen")
    @SequenceGenerator(name = "answers_reference_gen", sequenceName = "answers_reference_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "reference_text", length = 2000)
    private String referenceText;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

