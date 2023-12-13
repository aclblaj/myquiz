package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.AuthorErrors;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorErrorService {

    @Autowired
    AuthorErrorsRepository authorErrorsRepository;
    public void addAuthorError(Question question, String description) {
        AuthorErrors authorErrors = new AuthorErrors();
        authorErrors.setName(question.getAuthor());
        authorErrors.setInitials(question.getInitiale());
        authorErrors.setDescription(description);
        authorErrors.setRowNumber(question.getCrtNo());
        authorErrorsRepository.save(authorErrors);
    }
}
