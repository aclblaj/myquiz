package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.Quiz;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.app.mapper.CourseMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuizErrorRepository;
import com.unitbv.myquiz.app.repositories.QuizRepository;
import com.unitbv.myquiz.app.specifications.CourseSpecification;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.specifications.QuizErrorSpecification;
import com.unitbv.myquiz.app.specifications.QuizSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final AuthorRepository authorRepository;
    private final QuizAuthorRepository quizAuthorRepository;
    private final QuizRepository quizRepository;
    private final QuizErrorRepository quizErrorRepository;
    private final CourseMapper courseMapper;

    @Autowired
    public CourseService(CourseRepository courseRepository, QuestionRepository questionRepository,
                         AuthorRepository authorRepository, QuizAuthorRepository quizAuthorRepository,
                         QuizRepository quizRepository, QuizErrorRepository quizErrorRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.questionRepository = questionRepository;
        this.authorRepository = authorRepository;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
        this.quizErrorRepository = quizErrorRepository;
        this.courseMapper = courseMapper;
    }


    public List<CourseDto> getAllCourses() {
        List<CourseDto> courseDtos = new ArrayList<>();
        List<Course> courses = courseRepository.findAll();
        courses.forEach(course -> courseDtos.add(courseMapper.toDto(course)));
        log.atInfo().log("Found {} courses", courseDtos.size());
        return courseDtos;
    }


    @CacheEvict(value = "courseNames", allEntries = true)
    public void deleteCourseById(Long id) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            courseRepository.deleteById(id);
        } else {
            log.error("course {} not found", id);
        }
    }


    @CacheEvict(value = "courseNames", allEntries = true)
    public void deleteCourse(String selectedCourse) {
        try {
            Specification<Quiz> quizSpec = QuizSpecification.byCourse(selectedCourse);
            Optional<Quiz> first = quizRepository.findAll(quizSpec).stream().findFirst();
            if (first.isEmpty()) {
                log.error("No quiz found for course {}", selectedCourse);
                return;
            }
            Long quizId = first.get().getId();
            List<Long> quizAuthorIds = quizAuthorRepository.findAll(
                QuizAuthorSpecification.hasQuizId(quizId)
                    .and(QuizAuthorSpecification.fetchQuizErrors())
            ).stream().map(QuizAuthor::getId).toList();

            // Delete quiz errors using Specification
            quizAuthorIds.stream().forEach(quizAuthorId -> {
                Specification<QuizError> errorSpec = QuizErrorSpecification.byQuizAuthor(quizAuthorId);
                quizErrorRepository.deleteAll(quizErrorRepository.findAll(errorSpec));
            });

            quizAuthorIds.stream().forEach(quizAuthorId -> {
                // Delete questions using Specification
                Specification<Question> questionSpec = QuestionSpecification.byQuizAuthorId(quizAuthorId);
                questionRepository.deleteAll(questionRepository.findAll(questionSpec));

                QuizAuthor quizAuthor = quizAuthorRepository.findById(quizAuthorId).get();
                Author author = quizAuthor.getAuthor();
                long countAuthor = quizAuthorRepository.count(
                    QuizAuthorSpecification.hasAuthor(author)
                );
                if (countAuthor == 1) {
                    authorRepository.delete(author);
                    quizAuthorRepository.delete(quizAuthor);
                }
            });

            quizRepository.deleteById(quizId);
            Specification<Course> courseSpec = CourseSpecification.byCourseName(selectedCourse);
            List<Course> found = courseRepository.findAll(courseSpec);
            if (!found.isEmpty()) {
                courseRepository.deleteAll(found);
            } else {
                log.error("course {} not found", selectedCourse);
            }
        } catch (Exception e) {
            log.error("Error deleting course '{}': {}", selectedCourse, e.getMessage(), e);
        }
    }


    public CourseDto findById(Long id) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            return courseMapper.toDto(found.get());
        } else {
            log.error("course {} not found", id);
            return null;
        }
    }


    @CacheEvict(value = "courseNames", allEntries = true)
    public void updateCourse(Long id, CourseDto courseDto) {
        Optional<Course> found = courseRepository.findById(id);
        if (found.isPresent()) {
            Course course = found.get();
            course.setCourse(courseDto.getCourse());
            course.setDescription(courseDto.getDescription());
            course.setUniversityYear(courseDto.getUniversityYear());
            course.setSemester(courseDto.getSemester());
            course.setStudyYear(courseDto.getStudyYear());
            courseRepository.save(course);
            log.info("Course {} updated", id);
        } else {
            log.error("Course {} not found for update", id);
        }
    }


    @CacheEvict(value = "courseNames", allEntries = true)
    public CourseDto createCourse(CourseDto courseDto) {
        Specification<Course> spec = CourseSpecification.byCourseName(courseDto.getCourse());
        List<Course> found = courseRepository.findAll(spec);
        if (!found.isEmpty()) {
            log.error("Course {} already exists", courseDto.getCourse());
            courseDto = courseMapper.toDto(found.getFirst());
        } else {
            log.info("Creating new course {}", courseDto.getCourse());
            Course course = courseMapper.toEntity(courseDto);
            courseRepository.save(course);
            log.info("Course {} created", course.getId());
            courseDto = courseMapper.toDto(course);
        }
        return courseDto;
    }


    public CourseDto createCourseIfNotExists(CourseDto courseDto) {
        Specification<Course> spec = CourseSpecification.byCourseName(courseDto.getCourse());
        List<Course> found = courseRepository.findAll(spec);
        if (!found.isEmpty()) {
            Course course = found.getFirst();
            log.info("Course {} already exists", course.getId());
            return courseMapper.toDto(course);
        } else {
            return createCourse(courseDto);
        }
    }


    @Cacheable("courseNames")
    public List<String> getAllCourseNames() {
        return courseRepository.findAll().stream().map(Course::getCourse).toList();
    }


    public String getCourseName(Long courseId) {
        String courseName = "";
        var courseOpt = courseRepository.findById(courseId);
        if (!courseOpt.isEmpty()) {
            courseName = courseOpt.get().getCourse();
        }
        return courseName;
    }

}
