package com.unitbv.myquiz.app.entities;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * Primary persisted error entity attached to a single question.
 */
@Entity
@Table(name = "question_error")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"question"})
@NoArgsConstructor
@AllArgsConstructor
public class QuestionError {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_error_gen")
    @SequenceGenerator(name = "question_error_gen", sequenceName = "question_error_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "status", length = 20)
    private String status = ControllerSettings.ERROR_STATUS_OPEN;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public QuestionError(Question question, String description, Integer rowNumber) {
        this.question = question;
        this.description = description;
        this.rowNumber = rowNumber;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

