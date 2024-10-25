package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.AuthorErrorRepository;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AuthorServiceImpl implements AuthorService{
    private final QuizAuthorRepository quizAuthorRepository;
    Logger log = LoggerFactory.getLogger(AuthorServiceImpl.class.getName());
    AuthorRepository authorRepository;
    AuthorErrorRepository authorErrorRepository;
    QuestionRepository questionRepository;
    QuestionService questionService;
    String authorName;
    private ArrayList<Author> authors;

    @Lazy
    @Autowired
    public AuthorServiceImpl(
            AuthorRepository authorRepository,
            AuthorErrorRepository authorErrorRepository,
            QuestionService questionService, QuizAuthorRepository quizAuthorRepository) {
        this.authorRepository = authorRepository;
        this.authorErrorRepository = authorErrorRepository;
        this.questionService = questionService;
        this.quizAuthorRepository = quizAuthorRepository;
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
            log.atInfo().addArgument(author.getName())
               .log("Author '{}' already exists in the database");
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
        author.getQuizAuthors().forEach(quizAuthor -> {
            long noMC = quizAuthor.getQuestions().stream()
                                  .filter(q -> q.getType().equals(QuestionType.MULTICHOICE))
                                  .count();
            authorDto.setNumberOfMultipleChoiceQuestions(authorDto.getNumberOfMultipleChoiceQuestions()+noMC);
            long noTF = quizAuthor.getQuestions().stream()
                                  .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
                                  .count();
            authorDto.setNumberOfTrueFalseQuestions(authorDto.getNumberOfTrueFalseQuestions()+noTF);
            authorDto.setNumberOfErrors(authorDto.getNumberOfErrors()+quizAuthor.getQuizErrors().size());
            authorDto.setNumberOfQuestions(authorDto.getNumberOfQuestions() + noMC + noTF);
        });
//        authorDto.setNumberOfMultipleChoiceQuestions(
//                author.getQuestions().stream()
//                      .filter(q -> q.getType().equals(QuestionType.MULTICHOICE))
//                      .count()
//        );
//        authorDto.setNumberOfTrueFalseQuestions(
//                author.getQuestions().stream()
//                      .filter(q -> q.getType().equals(QuestionType.TRUEFALSE))
//                      .count()
//        );
        return authorDto;
    }

    @Override
    public Page<Author> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
        Pageable paging = MyUtil.getPageable(pageNo, pageSize, sortField, sortDirection);
        return authorRepository.findAll(paging);
    }

    @Override
    public void setAuthorsList(ArrayList<Author> authors) {
        this.authors = authors;
    }

    @Override
    public ArrayList<Author> getAuthorsList() {
        return authors;
    }

    @Override
    public void addAuthorToList(Author author) {
        if (authors == null) {
            authors = new ArrayList<>();
        }
        authors.add(author);
    }

    @Override
    public void deleteAuthorById(long id) {
        Author author = authorRepository.findById(id).orElse(null);
        if (author != null) {
            authorRepository.deleteById(id);
        } else {
            log.error("Author with id '{}' not found", id);
        }
    }

    @Override
    public boolean authorNameExists(String name) {
        boolean result = false;
        Author author = authorRepository.findByName(name).orElse(null);
        if (author != null) {
            result = true;
        }
        return result;
    }

    @Override
    public Author getAuthorByName(String name) {
        return authorRepository.findByName(name).orElse(null);
    }

    @Override
    public List<QuizAuthor> getQuizAuthorsForAuthorId(Long authorId) {
        return quizAuthorRepository.findByAuthorId(authorId);
    }

    @Override
    public void deleteQuizAuthorsByIds(List<Long> idsQA) {
        quizAuthorRepository.deleteAllById(idsQA);
    }

}
