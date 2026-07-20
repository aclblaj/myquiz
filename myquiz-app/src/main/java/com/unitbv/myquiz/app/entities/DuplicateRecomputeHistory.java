package com.unitbv.myquiz.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Persistent history record of a duplicate recompute run.
 * Stores all execution parameters and result metrics so past runs can be reviewed and deleted.
 */
@Entity
@Table(name = "duplicate_recompute_history",
        indexes = {
                @Index(name = "idx_dup_recompute_hist_course_id", columnList = "course_id"),
                @Index(name = "idx_dup_recompute_hist_saved_at", columnList = "saved_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateRecomputeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dup_recompute_history_gen")
    @SequenceGenerator(name = "dup_recompute_history_gen", sequenceName = "dup_recompute_history_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", length = 255)
    private String courseName;

    @Column(name = "question_bank_id")
    private Long questionBankId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "strategy", length = 50, nullable = false)
    private String strategy;

    @Column(name = "total_questions")
    private int totalQuestions;

    @Column(name = "multichoice_questions")
    private int multichoiceQuestions;

    @Column(name = "truefalse_questions")
    private int truefalseQuestions;

    @Column(name = "duplicate_links_removed")
    private int duplicateLinksRemoved;

    @Column(name = "duplicate_errors_removed")
    private int duplicateErrorsRemoved;

    @Column(name = "duplicate_errors_created")
    private int duplicateErrorsCreated;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "saved_at", updatable = false)
    private OffsetDateTime savedAt;

    @PrePersist
    protected void onCreate() {
        savedAt = OffsetDateTime.now();
    }
}

