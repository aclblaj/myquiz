package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.repositories.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class AuthorServiceImpl implements AuthorService{
    Logger log = LoggerFactory.getLogger(AuthorServiceImpl.class.getName());
    AuthorRepository authorRepository;
    String authorName;

    @Autowired
    public AuthorServiceImpl(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public String extractAuthorNameFromPath(String filePath) {
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

    public AuthorDto getAuthorDTO(Author author) {
        AuthorDto authorDto = new AuthorDto(author);
        authorDto.setNumberOfMultipleChoiceQuestions(
                author.getQuestions().stream()
                      .filter(q -> q.getType().equals(QuestionType.MULTICHOICE))
                      .count()
        );
        authorDto.setNumberOfTrueFalseQuestions(
                author.getQuestions().stream()
                      .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                      .count()
        );
        return authorDto;
    }

    @Override
    public Page<Author> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        return authorRepository.findAll(paging);
    }
}
