package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.AuthorErrors;
import com.unitbv.myquiz.repositories.AuthorErrorsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorErrorService {

    @Autowired
    AuthorErrorsRepository authorErrorsRepository;
    public void addAuthorError(String authorName, String initials, String description, int rowNumber) {
        AuthorErrors authorErrors = new AuthorErrors();
        authorErrors.setName(authorName);
        authorErrors.setInitials(initials);
        authorErrors.setDescription(description);
        authorErrors.setRowNumber(rowNumber);
        authorErrorsRepository.save(authorErrors);
    }
}
