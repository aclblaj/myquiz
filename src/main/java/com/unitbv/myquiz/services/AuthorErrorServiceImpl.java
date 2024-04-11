package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.AuthorError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class AuthorErrorServiceImpl implements AuthorErrorService {

    AuthorErrorRepository authorErrorRepository;

    String sourceFile;

    @Autowired
    public AuthorErrorServiceImpl(AuthorErrorRepository authorErrorRepository) {
        this.authorErrorRepository = authorErrorRepository;
    }
    @Override
    public void addAuthorError(Author author, Question question, String description) {
        AuthorError authorError = new AuthorError();
        authorError.setDescription(description);
        authorError.setRowNumber(question.getCrtNo());
        authorError.setAuthor(author);
        authorError.setSource(sourceFile);
        authorErrorRepository.save(authorError);
    }

    @Override
    public List<AuthorError> getErrorsForAuthorName(String authorName) {
        return authorErrorRepository.findAllByAuthor_NameContainsIgnoreCase(authorName);
    }

    @Override
    public List<AuthorError> getErrors() {
        return authorErrorRepository.findAllByOrderByAuthor_NameAsc();
    }

    @Override
    public void setSource(String filePath) {
        int pos = filePath.lastIndexOf(File.separator);
        String filename = filePath.substring(pos + 1);
        sourceFile = filename;
    }

    @Override
    public void deleteAll() {
        authorErrorRepository.deleteAll();
    }
}
