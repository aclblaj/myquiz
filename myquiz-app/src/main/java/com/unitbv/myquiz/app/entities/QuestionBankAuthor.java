package com.unitbv.myquiz.app.entities;

import com.unitbv.myquiz.api.types.TemplateType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_bank_author")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"author", "questionBank", "questions"})
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_bank_author_gen")
    @SequenceGenerator(name = "question_bank_author_gen", sequenceName = "question_bank_author_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id")
    private QuestionBank questionBank;

    @Column(name = "source", length = 500)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", length = 50)
    private TemplateType templateType;


    @OneToMany(mappedBy = "questionBankAuthor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

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

    // Business logic helper
    public String getAuthorName() {
        return author != null ? author.getName() : null;
    }
}

