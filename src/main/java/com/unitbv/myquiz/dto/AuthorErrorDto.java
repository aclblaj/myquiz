package com.unitbv.myquiz.dto;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;

import java.io.File;
@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor

public class AuthorErrorDto {

    private String courseName;
    private String quizName;
    private Long id;
    private Integer row;
    private String description;
    private String authorName;
    private String source;

    public AuthorErrorDto(QuizError error) {
        this.id = error.getId();
        this.row = error.getRowNumber();
        this.description = error.getDescription();
        QuizAuthor quizAuthor = error.getQuizAuthor();
        if (quizAuthor != null) {
            this.authorName = quizAuthor.getAuthor().getName();
            int pos = quizAuthor.getSource().lastIndexOf(File.separator);
            String filename = quizAuthor.getSource().substring(pos + 1);
            this.source = filename;
            this.courseName = quizAuthor.getQuiz().getCourse();
            this.quizName = quizAuthor.getQuiz().getName();
        }
    }
}
