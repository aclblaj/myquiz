package com.unitbv.myquiz.services;

import com.unitbv.myquiz.dto.CourseDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Course;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.CourseRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizErrorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private QuizAuthorRepository quizAuthorRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizErrorRepository quizErrorRepository;

    public List<CourseDto> getAllCourses() {
        List<CourseDto> courseDtos = new ArrayList<>();
        List<Course> courses = (List<Course>) courseRepository.findAll();
        courses.stream().forEach(
                course -> {
                    CourseDto courseDto = new CourseDto(
                            course.getId(),
                            course.getCourse(),
                            course.getDescription(),
                            course.getUniversityYear(),
                            course.getSemester(),
                            course.getStudy_year()
                    );
                    courseDtos.add(courseDto);
                }
        );
        return courseDtos;
    }

    @Override
    public void deleteCourseById(Long id) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            courseRepository.deleteById(id);
        } else {
            log.error("course {} not found", id);
        }
    }

    @Override
    public void deleteCourse(String selectedCourse) {

        // find quiz by course
        Long quizId = quizRepository.findQuizIdByCourse(selectedCourse)
                                      .stream()
                                      .findFirst()
                                        .get().getId();
        // find all quiz authors for quiz
        List<Long> quizAuthorIds = quizAuthorRepository.findQuizAuthorByQuizId(quizId)
                                                       .stream().map(QuizAuthor::getId).toList();
        // for each quiz author delete quiz errors
        quizAuthorIds.stream().forEach(
                quizAuthorId -> quizErrorRepository.deleteQuizErrorsByQuizAuthorId(quizAuthorId)
        );
        // for each quiz author delete author if appear only once in quiz author
        quizAuthorIds.stream().forEach(
                quizAuthorId -> {
                    questionRepository.deleteQuestionsByQuizAuthorId(quizAuthorId);

                    QuizAuthor quizAuthor = quizAuthorRepository.findById(quizAuthorId).get();
                    Author author = quizAuthor.getAuthor();
                    long countAuthor = quizAuthorRepository.countByAuthor(author);
                    if (countAuthor == 1) {
                        authorRepository.delete(author);
                        quizAuthorRepository.delete(quizAuthor);
                    }
                }
        );

        // delete quiz by course
        quizRepository.deleteById(quizId);



        // delete course details
        Optional<Course> found = courseRepository.findByCourse(selectedCourse);
        if (found.isPresent()) {
            courseRepository.delete(found.get());
        } else {
            log.error("course {} not found", selectedCourse);
        }
    }


}
