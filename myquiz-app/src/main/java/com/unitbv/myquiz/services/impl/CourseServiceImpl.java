package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Course;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.CourseRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizErrorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.services.CourseService;
import com.unitbv.myquizapi.dto.CourseDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

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
        log.atInfo().log("Found {} courses", courseDtos.size());
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
        try {
            // find quiz by course
            Long quizId = quizRepository.findQuizIdByCourse(selectedCourse)
                                          .stream()
                                          .findFirst()
                                            .get().getId();
            // find all quiz authors for quiz
            List<Long> quizAuthorIds = quizAuthorRepository.findWithQuizErrorsByQuizId(quizId)
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
        } catch (Exception e) {
            log.error("Error deleting course '{}': {}", selectedCourse, e.getMessage(), e);
        }
    }

    @Override
    public CourseDto findById(Long id) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            Course course = found.get();
            return new CourseDto(
                    course.getId(),
                    course.getCourse(),
                    course.getDescription(),
                    course.getUniversityYear(),
                    course.getSemester(),
                    course.getStudy_year()
            );
        } else {
            log.error("course {} not found", id);
            return null;
        }
    }

    @Override
    public void updateCourse(Long id, com.unitbv.myquizapi.dto.CourseDto courseDto) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            Course course = found.get();
            course.setCourse(courseDto.getCourse());
            course.setDescription(courseDto.getDescription());
            course.setUniversityYear(courseDto.getUniversityYear());
            course.setSemester(courseDto.getSemester());
            course.setStudy_year(courseDto.getStudy_year());
            courseRepository.save(course);
            log.info("Course {} updated", id);
        } else {
            log.error("Course {} not found for update", id);
        }
    }

    @Override
    public CourseDto createCourse(CourseDto courseDto) {
        Course course = new Course();
        course.setCourse(courseDto.getCourse());
        course.setDescription(courseDto.getDescription());
        course.setUniversityYear(courseDto.getUniversityYear());
        course.setSemester(courseDto.getSemester());
        course.setStudy_year(courseDto.getStudy_year());
        courseRepository.save(course);
        log.info("Course {} created", course.getId());
        return new CourseDto(
                course.getId(),
                course.getCourse(),
                course.getDescription(),
                course.getUniversityYear(),
                course.getSemester(),
                course.getStudy_year()
        );
    }

    @Override
    public CourseDto createCourseIfNotExists(CourseDto courseDto) {
        CourseDto existingCourse = null;
        Optional<Course> found = courseRepository.findByCourse(courseDto.getCourse());
        if (found.isPresent()) {
            Course course = found.get();
            existingCourse = new CourseDto(
                    course.getId(),
                    course.getCourse(),
                    course.getDescription(),
                    course.getUniversityYear(),
                    course.getSemester(),
                    course.getStudy_year()
            );
            log.info("Course {} already exists", course.getId());
        } else {
            existingCourse = createCourse(courseDto);
        }
        return existingCourse;
    }


}
