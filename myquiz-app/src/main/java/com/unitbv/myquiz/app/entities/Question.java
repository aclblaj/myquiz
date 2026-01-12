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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question", indexes = {
    @Index(name = "idx_question_quiz_author_id", columnList = "quiz_author_id"),
    @Index(name = "idx_question_type", columnList = "type")
})
public class Question {
    private int crtNo;
    private String chapter;
    private String title;
    @Column(length = 1024)
    private String text;
    @Enumerated(EnumType.STRING)
    private QuestionType type;
    private Double weightResponse1;
    @Column(length = 1024)
    private String response1;
    private Double weightResponse2;
    @Column(length = 1024)
    private String response2;
    private Double weightResponse3;
    @Column(length = 1024)
    private String response3;
    private Double weightResponse4;
    @Column(length = 1024)
    private String response4;

    private Double weightTrue;
    private Double weightFalse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_author_id")
    private QuizAuthor quizAuthor;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_gen")
    @SequenceGenerator(name = "question_gen", sequenceName = "question_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionError> questionErrors = new ArrayList<>();

    public Question() {
    }

    public Double getWeightTrue() {
        return weightTrue;
    }

    public void setWeightTrue(Double weightTrue) {
        this.weightTrue = weightTrue;
    }

    public Double getWeightFalse() {
        return weightFalse;
    }

    public void setWeightFalse(Double weightFalse) {
        this.weightFalse = weightFalse;
    }

    public int getCrtNo() {
        return crtNo;
    }

    public void setCrtNo(int crtNo) {
        this.crtNo = crtNo;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String course) {
        this.chapter = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Double getWeightResponse1() {
        return weightResponse1;
    }

    public void setWeightResponse1(Double weightResponse1) {
        this.weightResponse1 = weightResponse1;
    }

    public String getResponse1() {
        return response1;
    }

    public void setResponse1(String response1) {
        this.response1 = response1;
    }

    public Double getWeightResponse2() {
        return weightResponse2;
    }

    public void setWeightResponse2(Double weightResponse2) {
        this.weightResponse2 = weightResponse2;
    }

    public String getResponse2() {
        return response2;
    }

    public void setResponse2(String response2) {
        this.response2 = response2;
    }

    public Double getWeightResponse3() {
        return weightResponse3;
    }

    public void setWeightResponse3(Double weightResponse3) {
        this.weightResponse3 = weightResponse3;
    }

    public String getResponse3() {
        return response3;
    }

    public void setResponse3(String response3) {
        this.response3 = response3;
    }

    public Double getWeightResponse4() {
        return weightResponse4;
    }

    public void setWeightResponse4(Double weightResponse4) {
        this.weightResponse4 = weightResponse4;
    }

    public String getResponse4() {
        return response4;
    }

    public void setResponse4(String response4) {
        this.response4 = response4;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRow() {
        // Return the row number or unique identifier for this question
        return crtNo;
    }

    @Override
    public String toString() {
        return "Question{" + "crtNo=" + crtNo + ", title='" + title + '\'' + ", text='" + text + '\'' + ", type=" + type + ", weightResponse1=" + weightResponse1 + ", response1='" + response1 + '\'' + ", weightResponse2=" + weightResponse2 + ", response2='" + response2 + '\'' + ", weightResponse3=" + weightResponse3 + ", response3='" + response3 + '\'' + ", weightResponse4=" + weightResponse4 + ", response4='" + response4 + '\'' + ", id=" + id + '}';
    }

    public QuizAuthor getQuizAuthor() {
        return quizAuthor;
    }

    public void setQuizAuthor(QuizAuthor quizAuthor) {
        this.quizAuthor = quizAuthor;
    }

    public List<QuestionError> getQuestionErrors() {
        return questionErrors;
    }

    public void setQuestionErrors(List<QuestionError> questionErrors) {
        this.questionErrors = questionErrors;
    }

    public void addQuestionError(QuestionError questionError) {
        questionErrors.add(questionError);
        questionError.setQuestion(this);
    }

    public void removeQuestionError(QuestionError questionError) {
        questionErrors.remove(questionError);
        questionError.setQuestion(null);
    }
}
