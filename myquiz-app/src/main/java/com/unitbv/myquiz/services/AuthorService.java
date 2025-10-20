package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquizapi.dto.AuthorDto;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
public interface AuthorService {
    String extractAuthorNameFromPath(String filePath);
    String extractInitials(String authorName);
    Author saveAuthor(Author author);
    List<Author> getAllAuthors();
    void deleteAll();
    AuthorDto getAuthorDTO(Author author, String courseName);
    Page<Author> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection);
    Page<Author> findPaginatedFiltered(String course, int pageNo, int pageSize, String sortField, String sortDirection);
    void setAuthorsList(ArrayList<Author> authors);
    ArrayList<Author> getAuthorsList();
    void addAuthorToList(Author author);
    void deleteAuthorById(long id);

    boolean authorNameExists(String name);

    Author getAuthorByName(String name);

    List<QuizAuthor> getQuizAuthorsForAuthorId(Long authorId);

    void deleteQuizAuthorsByIds(List<Long> idsQA);

    List<String> getCourseNames();

    void deleteAuthorsWithoutQuiz();

    void prepareFilterAuthorsModel(Model model, String selectedCourse);

    ArrayList<String> getCoursesNamesAsStrings();
    void prepareAuthorModelData(Model model, String authorName);

    List<Author> getAuthorsByCourse(String course);

    Author getAuthorById(Long selectedAuthorObj);
}
