package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class AuthorErrorServiceImpl implements AuthorErrorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorErrorServiceImpl.class);
    AuthorErrorRepository authorErrorRepository;

    String sourceFile;

    @Autowired
    public AuthorErrorServiceImpl(AuthorErrorRepository authorErrorRepository) {
        this.authorErrorRepository = authorErrorRepository;
    }
    @Override
    public void addAuthorError(QuizAuthor quizAuthor, Question question, String description) {
        QuizError quizError = new QuizError();
        quizError.setDescription(description);
        quizError.setRowNumber(question.getCrtNo());
        quizError.setQuizAuthor(quizAuthor);
        quizAuthor.getQuizErrors().add(quizError);
        log.trace("Author error added: {} quizAuthor", quizError, quizError.getQuizAuthor().getAuthor().getName());
    }

    @Override
    public List<QuizError> getErrorsForAuthorName(String authorName) {
        return authorErrorRepository.findAllByQuizAuthor_Author_NameContainsIgnoreCase(authorName);
    }

    @Override
    public List<QuizError> getErrors() {
        return authorErrorRepository.findAllByOrderByQuizAuthor_Author_NameAsc();
    }

    @Override
    public void setSource(String filePath) {
        int pos = filePath.lastIndexOf(File.separator);
        String filename = filePath.substring(pos + 1);
        sourceFile = filename;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public void deleteAll() {
        authorErrorRepository.deleteAll();
    }

    @Override
    public void saveAllAuthorErrors(List<QuizError> quizErrors) {
        authorErrorRepository.saveAll(quizErrors);
    }
}
