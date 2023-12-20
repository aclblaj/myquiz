package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.AuthorError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorErrorService {

    @Autowired
    AuthorErrorRepository authorErrorRepository;
    public void addAuthorError(Question question, String description) {
        AuthorError authorError = new AuthorError();
        authorError.setName(question.getAuthor());
        authorError.setInitials(question.getInitiale());
        authorError.setDescription(description);
        authorError.setRowNumber(question.getCrtNo());
        authorErrorRepository.save(authorError);
    }
}
