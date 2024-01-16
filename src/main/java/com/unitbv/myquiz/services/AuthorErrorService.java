package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.AuthorError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorErrorService {

    @Autowired
    AuthorErrorRepository authorErrorRepository;
    public void addAuthorError(Author author, Question question, String description) {
        AuthorError authorError = new AuthorError();
        authorError.setDescription(description);
        authorError.setRowNumber(question.getCrtNo());
        authorError.setAuthor(author);
        authorErrorRepository.save(authorError);
    }

    public List<AuthorError> getErrorsForAuthorName(String authorName) {
        return authorErrorRepository.findAllByAuthor_NameContainsIgnoreCase(authorName);
    }

    public List<AuthorError> getErrors() {
        return authorErrorRepository.findAllByOrderByAuthor_NameAsc();
    }
}
