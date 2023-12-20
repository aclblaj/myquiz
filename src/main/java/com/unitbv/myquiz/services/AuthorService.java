package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.repositories.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {

    @Autowired
    AuthorRepository authorRepository;
    String authorName;
    String initials;

    public String getAuthorName(String filePath) {
        String authorPlus = filePath.substring(filePath.indexOf("inpQ1") + 6);
        String authorName = authorPlus.substring(0, authorPlus.indexOf("_"));
        return authorName;
    }

    public String extractInitials(String authorName) {
        String initials = "";
        if (authorName.length() > 0) {
            String[] split = authorName.split(" ");
            for (String s : split) {
                initials += s.charAt(0);
            }
        }
        return initials;
    }

    public Author saveAuthor(Author author) {
        return authorRepository.save(author);
    }
}
