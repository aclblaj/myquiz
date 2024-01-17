package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.repositories.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class AuthorService {

    Logger log = LoggerFactory.getLogger(AuthorService.class.getName());

    @Autowired
    AuthorRepository authorRepository;
    String authorName;

    public String getAuthorName(String filePath) {
        authorName = MyUtil.USER_NAME_NOT_DETECTED;

        Path path = Paths.get(filePath);
        if (path.toFile().exists()) {
            String lastDirectory = path.getParent().getFileName().toString();
            int endIndex = lastDirectory.indexOf("_");
            if (endIndex != -1) {
                authorName = lastDirectory.substring(0, endIndex);
            } else {
                log.error("Directory name '{}' not in the correct format (e.g.: 'John Doe_123'), use default '{}'"
                        , lastDirectory, authorName);
            }

        } else {
            log.error("Directory not found: {}", filePath);
        }
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
        Author authorDb = authorRepository.findByName(author.getName()).orElse(null);
        if (authorDb != null) {
            log.info("Author '{}' already exists in the database", author.getName());
            return authorDb;
        }
        return authorRepository.save(author);
    }

    public List<Author> getAllAuthors() {
        try {
            return authorRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting authors: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteAll() {
        authorRepository.deleteAll();
    }
}
