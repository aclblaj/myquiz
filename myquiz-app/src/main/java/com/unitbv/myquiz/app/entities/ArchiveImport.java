package com.unitbv.myquiz.app.entities;

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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "archive_import", indexes = {@Index(name = "idx_archive_import_size", columnList = "file_size"), @Index(name = "idx_archive_import_created_at", columnList = "processed_at")})
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ArchiveImport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "archive_import_gen")
    @SequenceGenerator(name = "archive_import_gen", sequenceName = "archive_import_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id")
    private QuestionBank questionBank;

    public ArchiveImport(String fileName, Long fileSize, QuestionBank questionBank) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.questionBank = questionBank;
    }

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = OffsetDateTime.now();
        }
    }
}


