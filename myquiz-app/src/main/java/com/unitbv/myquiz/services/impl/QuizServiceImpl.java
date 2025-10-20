package com.unitbv.myquiz.services.impl;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.CourseRepository;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquizapi.dto.AuthorDto;
import com.unitbv.myquizapi.dto.QuestionDto;
import com.unitbv.myquizapi.dto.QuizDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {

    QuizRepository quizRepository;
    QuizAuthorRepository quizAuthorRepository;
    QuestionRepository questionRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository, QuizAuthorRepository quizAuthorRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.quizAuthorRepository = quizAuthorRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public Quiz createQuizz(String courseName, String quizName, long year) {
        Quiz quiz;
        Optional<Quiz> searchQuiz = quizRepository.findByNameAndCourseAndAndYear(quizName, courseName, year);
        if (!searchQuiz.isPresent()) {
            Quiz newQuiz = new Quiz();
            newQuiz.setName(quizName);
            newQuiz.setCourse(courseName);
            newQuiz.setYear(year);
            quiz = quizRepository.save(newQuiz);
        } else {
            quiz = searchQuiz.get();
        }
        return quiz;
    }

    @Override
    public void deleteAll() {
        quizRepository.deleteAll();
    }

    @Override
    public List<QuizDto> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAll();
        quizzes.sort((q1, q2) -> q1.getCourse().compareTo(q2.getCourse()));
        return quizzes.stream()
            .map(quiz -> {
                QuizDto dto = new QuizDto();
                dto.setId(quiz.getId());
                dto.setName(quiz.getName());
                dto.setCourse(quiz.getCourse());
                dto.setYear(quiz.getYear());
                // Fetch questions for this quiz
                List<Question> questions = questionRepository.findByQuizAuthor_Quiz_Id(quiz.getId());
                int mcCount = 0;
                int tfCount = 0;
                for (Question question : questions) {
                    if (question.getType() == QuestionType.MULTICHOICE) {
                        mcCount++;
                    } else if (question.getType() == QuestionType.TRUEFALSE) {
                        tfCount++;
                    }
                }
                dto.setMcQuestionsCount(mcCount);
                dto.setTfQuestionsCount(tfCount);
                // count authors
                List<QuizAuthor> quizAuthors = quizAuthorRepository.findWithQuestionsAndQuizErrorsByQuizId(quiz.getId());
                dto.setNoAuthors(quizAuthors.size());
                return dto;
            })
            .toList();
    }

    @Override
    @Transactional
    public void deleteQuizById(Long id) {
        quizAuthorRepository.deleteAllByQuizId(id);
        quizRepository.deleteById(id);
    }

    @Override
    public QuizDto getQuizById(Long id) {
        Optional<Quiz> quiz = quizRepository.findById(id);
        if (quiz.isPresent()) {
            Quiz q = quiz.get();
            QuizDto dto = new QuizDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourse());
            dto.setYear(q.getYear());
            
            // Fetch questions for this quiz
            List<Question> questions = questionRepository.findByQuizAuthor_Quiz_Id(id);
            
            // Separate MC and TF questions and convert to DTOs
            List<QuestionDto> questionsMC = new ArrayList<>();
            List<QuestionDto> questionsTF = new ArrayList<>();
            
            for (Question question : questions) {
                QuestionDto questionDto = convertToQuestionDto(question);
                
                if (question.getType() == QuestionType.MULTICHOICE) {
                    questionsMC.add(questionDto);
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    questionsTF.add(questionDto);
                }
            }
            
            dto.setQuestionsMC(questionsMC);
            dto.setQuestionsTF(questionsTF);
            // Fetch authors with questions and quizErrors eagerly loaded
            List<QuizAuthor> quizAuthors = quizAuthorRepository.findWithQuestionsAndQuizErrorsByQuizId(id);
            dto.setNoAuthors(quizAuthors.size());
            // If you need to access quizErrors or questions, use quizAuthors here

            return dto;
        }
        return null;
    }

    private QuestionDto convertToQuestionDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setTitle(question.getTitle());
        dto.setChapter(question.getChapter());
        dto.setRow(question.getCrtNo());
        
        // Set author name if available
        if (question.getQuizAuthor() != null && question.getQuizAuthor().getAuthor() != null) {
            dto.setAuthorName(question.getQuizAuthor().getAuthor().getName());
        }
        
        // Set responses and weights
        dto.setResponse1(question.getResponse1());
        dto.setResponse2(question.getResponse2());
        dto.setResponse3(question.getResponse3());
        dto.setResponse4(question.getResponse4());
        dto.setWeightResponse1(question.getWeightResponse1());
        dto.setWeightResponse2(question.getWeightResponse2());
        dto.setWeightResponse3(question.getWeightResponse3());
        dto.setWeightResponse4(question.getWeightResponse4());
        dto.setWeightTrue(question.getWeightTrue());
        dto.setWeightFalse(question.getWeightFalse());
        
        return dto;
    }

    @Override
    public int getCompareTo(QuizDto q1, QuizDto q2) {
        if (q1 == null || q2 == null) {
            return 0;
        }
        return q1.getCourse().compareTo(q2.getCourse());
    }

    @Override
    public Quiz updateQuiz(Long id, String course, String name, Long year) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isPresent()) {
            Quiz quiz = quizOptional.get();
            quiz.setCourse(course);
            quiz.setName(name);
            quiz.setYear(year);
            return quizRepository.save(quiz);
        }
        return null;
    }

    @Override
    public List<QuizDto> getQuizzesByCourse(String course) {
        List<Quiz> quizzes = quizRepository.findQuizIdByCourse(course);
        return quizzes.stream()
            .map(quiz -> {
                QuizDto dto = new QuizDto();
                dto.setId(quiz.getId());
                dto.setName(quiz.getName());
                dto.setCourse(quiz.getCourse());
                dto.setYear(quiz.getYear());
                // Get source file from the first quiz author if available
                if (quiz.getQuizAuthors() != null && !quiz.getQuizAuthors().isEmpty()) {
                    dto.setSourceFile(quiz.getQuizAuthors().iterator().next().getSource());
                }
                // Eagerly fetch and set authors
                List<AuthorDto> authorDtos = quiz.getQuizAuthors().stream()
                    .map(author -> new AuthorDto(author.getId(), author.getName(), author.getSource()))
                    .collect(Collectors.toList());
                dto.setAuthors(authorDtos);
                // Eagerly fetch and set MC questions
                List<QuestionDto> mcQuestions = questionRepository.findByQuizAuthor_Quiz_Id(quiz.getId()).stream()
                    .filter(q -> q.getType() == QuestionType.MULTICHOICE)
                    .map(q -> new QuestionDto(q.getId(), q.getChapter(), q.getRow(), q.getTitle(), q.getText(),
                        q.getResponse1(), q.getResponse2(), q.getResponse3(), q.getResponse4(),
                        q.getWeightResponse1(), q.getWeightResponse2(), q.getWeightResponse3(), q.getWeightResponse4(),
                        q.getAuthor() != null ? q.getAuthor().getName() : null))
                    .collect(Collectors.toList());
                dto.setQuestionsMC(mcQuestions);
                // Eagerly fetch and set TF questions
                List<QuestionDto> tfQuestions = questionRepository.findByQuizAuthor_Quiz_Id(quiz.getId()).stream()
                    .filter(q -> q.getType() == QuestionType.TRUEFALSE)
                    .map(q -> new QuestionDto(q.getId(), q.getChapter(), q.getTitle(), q.getRow(), q.getText(),
                        q.getWeightTrue(), q.getWeightFalse(), q.getResponse1(),
                        q.getAuthor() != null ? q.getAuthor().getName() : null))
                    .collect(Collectors.toList());
                dto.setQuestionsTF(tfQuestions);
                // Remove authorErrors logic (or replace with correct repository if available)
                // dto.setAuthorErrorDtos(new ArrayList<>()); // Set empty or fetch if repository exists
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<QuizDto> getQuizzesByCourseId(Long courseId) {
        var courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) return List.of();
        String courseName = courseOpt.get().getCourse();
        List<Quiz> quizzes = quizRepository.findAllByCourse(courseName);
        return quizzes.stream().map(q -> {
            QuizDto dto = new QuizDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourse());
            dto.setYear(q.getYear());
            return dto;
        }).toList();
    }

    @Override
    public List<?> getQuestionsByQuizId(Long id) {
        return questionRepository.findByQuizAuthor_Quiz_Id(id);
    }
}
