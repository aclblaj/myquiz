package com.unitbv.myquiz.app.entities;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "question_bank")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"questionBankAuthors", "archiveImports", "course"})
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_bank_gen")
    @SequenceGenerator(name = "question_bank_gen", sequenceName = "question_bank_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_year", length = 20)
    private StudyYear studyYear;

    @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionBankAuthor> questionBankAuthors = new HashSet<>();

    @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArchiveImport> archiveImports = new HashSet<>();

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

    // Helper methods
    public List<Question> getQuestionsByType(QuestionType type) {
        return questionBankAuthors.stream().flatMap(qba -> qba.getQuestions().stream()).filter(q -> q.getType() == type).toList();
    }

    public List<QuestionError> getAllErrors() {
        return questionBankAuthors.stream().flatMap(qba -> qba.getQuestions().stream()).flatMap(question -> question.getQuestionErrors().stream()).toList();
    }

    /**
     * Backward-compatible accessor used by existing DTO mapping and templates.
     */
    @Transient
    public String getCourseName() {
        return course != null ? course.getCourse() : null;
    }
}

