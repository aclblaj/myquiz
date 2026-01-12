package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.ArrayList;

@Schema(description = "Quiz DTO for quiz management")
public class QuizDto {

    @Schema(description = "Unique identifier of the quiz")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Quiz name", required = true)
    @NotBlank(message = "Quiz name cannot be blank")
    @Size(max = 255, message = "Quiz name cannot exceed 255 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Course name")
    @JsonProperty("course")
    private String course;

    @Schema(description = "Quiz year")
    @JsonProperty("year")
    private Long year;

    @Schema(description = "Source file")
    @JsonProperty("sourceFile")
    private String sourceFile;

    @Schema(description = "Quiz author ID")
    @JsonProperty("quizAuthorId")
    private Long quizAuthorId;

    @Schema(description = "Number of authors")
    @JsonProperty("noAuthors")
    private Integer noAuthors;

    @Schema(description = "Multiple choice questions")
    @JsonProperty("questionsMultichoice")
    private List<QuestionDto> questionsMultichoice;

    @Schema(description = "True/false questions")
    @JsonProperty("questionsTruefalse")
    private List<QuestionDto> questionsTruefalse;

    @Schema(description = "Multiple choice question DTOs")
    @JsonProperty("questionDtosMultichoice")
    private List<QuestionDto> questionDtosMultichoice;

    @Schema(description = "True/false question DTOs")
    @JsonProperty("questionDtosTruefalse")
    private List<QuestionDto> questionDtosTruefalse;

    @Schema(description = "Author error DTOs")
    @JsonProperty("authorErrorDtos")
    private List<AuthorErrorDto> authorErrorDtos;

    @Schema(description = "Authors")
    @JsonProperty("authors")
    private List<AuthorDto> authors;

    @Schema(description = "Number of MC questions")
    @JsonProperty("mcQuestionsCount")
    private int mcQuestionsCount;

    @Schema(description = "Number of TF questions")
    @JsonProperty("tfQuestionsCount")
    private int tfQuestionsCount;

    // Default constructor
    public QuizDto() {}

    // Constructor with basic fields - entity conversion will be handled in service layer
    public QuizDto(Long id, String name, String course, Long year) {
        this.id = id;
        this.name = name;
        this.course = course;
        this.year = year;
        this.noAuthors = 0;
    }

    // Static method to convert list of basic quiz data to DTOs
    public static List<QuizDto> toDtoList(List<Object[]> quizData) {
        List<QuizDto> dtos = new ArrayList<>();
        for (Object[] data : quizData) {
            QuizDto dto = new QuizDto();
            dto.setId((Long) data[0]);
            dto.setName((String) data[1]);
            dto.setCourse((String) data[2]);
            dto.setYear((Long) data[3]);
            dtos.add(dto);
        }
        return dtos;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Long getYear() {
        return year;
    }

    public void setYear(Long year) {
        this.year = year;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Long getQuizAuthorId() {
        return quizAuthorId;
    }

    public void setQuizAuthorId(Long quizAuthorId) {
        this.quizAuthorId = quizAuthorId;
    }

    public Integer getNoAuthors() {
        return noAuthors;
    }

    public void setNoAuthors(Integer noAuthors) {
        this.noAuthors = noAuthors;
    }

    public List<QuestionDto> getQuestionsMultichoice() {
        if (questionsMultichoice == null) {
            questionsMultichoice = new ArrayList<>();
        }
        return questionsMultichoice;
    }

    public void setQuestionsMultichoice(List<QuestionDto> questionsMultichoice) {
        this.questionsMultichoice = questionsMultichoice != null ? questionsMultichoice : new ArrayList<>();
    }

    public List<QuestionDto> getQuestionsTruefalse() {
        if (questionsTruefalse == null) {
            questionsTruefalse = new ArrayList<>();
        }
        return questionsTruefalse;
    }

    public void setQuestionsTruefalse(List<QuestionDto> questionsTruefalse) {
        this.questionsTruefalse = questionsTruefalse != null ? questionsTruefalse : new ArrayList<>();
    }

    public List<QuestionDto> getQuestionDtosMultichoice() {
        return questionDtosMultichoice;
    }

    public void setQuestionDtosMultichoice(List<QuestionDto> questionDtosMultichoice) {
        this.questionDtosMultichoice = questionDtosMultichoice;
    }

    public List<QuestionDto> getQuestionDtosTruefalse() {
        return questionDtosTruefalse;
    }

    public void setQuestionDtosTruefalse(List<QuestionDto> questionDtosTruefalse) {
        this.questionDtosTruefalse = questionDtosTruefalse;
    }

    public List<AuthorErrorDto> getAuthorErrorDtos() {
        return authorErrorDtos;
    }

    public void setAuthorErrorDtos(List<AuthorErrorDto> authorErrorDtos) {
        this.authorErrorDtos = authorErrorDtos;
    }

    public List<AuthorDto> getAuthors() {
        return authors;
    }

    public List<AuthorInfo> getAuthorInfos() {
        List<AuthorInfo> authorInfos = new ArrayList<>();
        if (authors != null) {
            for (AuthorDto authorDto : authors) {
                AuthorInfo authorInfo = new AuthorInfo();
                authorInfo.setId(authorDto.getId());
                authorInfo.setName(authorDto.getName());
                authorInfos.add(authorInfo);
            }
        }
        return authorInfos;
    }

    public void setAuthors(List<AuthorDto> authors) {
        this.authors = authors;
    }

    public int getMcQuestionsCount() {
        return mcQuestionsCount;
    }

    public void setMcQuestionsCount(int mcQuestionsCount) {
        this.mcQuestionsCount = mcQuestionsCount;
    }

    public int getTfQuestionsCount() {
        return tfQuestionsCount;
    }

    public void setTfQuestionsCount(int tfQuestionsCount) {
        this.tfQuestionsCount = tfQuestionsCount;
    }

    // Alias method for template compatibility
    public List<AuthorErrorDto> getAuthorErrors() {
        return authorErrorDtos != null ? authorErrorDtos : new ArrayList<>();
    }

    public void setAuthorErrors(List<AuthorErrorDto> authorErrors) {
        this.authorErrorDtos = authorErrors;
    }

    // For template compatibility
    public List<QuestionDto> getQuestionsMC() {
        return getQuestionsMultichoice();
    }
    public void setQuestionsMC(List<QuestionDto> questionsMC) {
        setQuestionsMultichoice(questionsMC);
    }
    public List<QuestionDto> getQuestionsTF() {
        return getQuestionsTruefalse();
    }
    public void setQuestionsTF(List<QuestionDto> questionsTF) {
        setQuestionsTruefalse(questionsTF);
    }

    @Override
    public String toString() {
        return "QuizDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", course='" + course + '\'' +
                ", year=" + year +
                '}';
    }
}
