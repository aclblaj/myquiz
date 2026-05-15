package com.unitbv.myquiz.app.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "questionBanks")
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_gen")
    @SequenceGenerator(name = "course_gen", sequenceName = "course_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "course", nullable = false, length = 200)
    private String course;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "university_year", length = 20)
    private String universityYear;

    @Column(name = "semester", length = 20)
    private String semester;


    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<QuestionBank> questionBanks = new HashSet<>();

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

    @Transient
    public Set<QuestionBank> getQuestionBanks() {
        Set<QuestionBank> banks = new HashSet<>();
        for (QuestionBank questionBank : questionBanks) {
            QuestionBank dmyQB = new QuestionBank();
            dmyQB.setId(questionBank.getId());
            dmyQB.setName(questionBank.getName());
            dmyQB.setCourse(questionBank.getCourse());
            dmyQB.setStudyYear(questionBank.getStudyYear());
            dmyQB.setArchiveImports(questionBank.getArchiveImports());
            dmyQB.setQuestionBankAuthors(questionBank.getQuestionBankAuthors());
            banks.add(dmyQB);
        }
        return banks;
    }
}
