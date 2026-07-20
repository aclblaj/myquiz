package com.unitbv.myquiz.app.upload.domain.policy;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.services.AuthorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class UploadAuthorResolutionPolicy {
    private static final Logger logger = LoggerFactory.getLogger(UploadAuthorResolutionPolicy.class);

    private final AuthorService authorService;

    public UploadAuthorResolutionPolicy(AuthorService authorService) {
        this.authorService = authorService;
    }

    public Map<String, Author> loadAuthorCacheByInitials() {
        Map<String, Author> authorsByInitials = new HashMap<>();
        for (AuthorInfo authorInfo : authorService.getAllAuthorsBasic()) {
            if (authorInfo.getId() == null || authorInfo.getInitials() == null || authorInfo.getInitials().isBlank()) {
                continue;
            }
            Author author = authorService.findAuthorEntityById(authorInfo.getId());
            if (author != null) {
                authorsByInitials.putIfAbsent(authorInfo.getInitials().trim().toUpperCase(Locale.ROOT), author);
            }
        }
        return authorsByInitials;
    }

    public Author resolveAuthor(String initials, Map<String, Author> authorsByInitials) {
        if (initials != null && !initials.isBlank()) {
            String normalizedInitials = initials.trim().toUpperCase(Locale.ROOT);
            Author existing = authorsByInitials.get(normalizedInitials);
            if (existing != null) {
                logger.atInfo().addArgument(normalizedInitials).log("Resolved author by initials: {}");
                return existing;
            }
            logger.atWarn().addArgument(normalizedInitials).log("Author initials '{}' not found in cache - assigning to dummy author");
        } else {
            logger.atWarn().log("Blank/null author initials encountered - assigning to dummy author");
        }
        return authorsByInitials.computeIfAbsent("__DUMMY__", key -> authorService.findOrCreateDummyAuthor());
    }
}

