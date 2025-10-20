package com.unitbv.myquiz.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquizapi.dto.QuestionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for the question editor functionality
 */
@Controller
@RequestMapping("/question-editor")
public class QuestionEditorController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionEditorController.class);
    
    private final QuestionService questionService;
    private final AuthorService authorService;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuestionEditorController(QuestionService questionService, AuthorService authorService) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Show the question editor with a new question
     */
    @GetMapping
    public String showEditor(Model model) {
        logger.info("Showing question editor with new question");
        
        // Create a sample question for new question creation
        QuestionDto sampleQuestion = createSampleQuestion();
        
        model.addAttribute("question", sampleQuestion);
        model.addAttribute("questionJson", formatQuestionAsJson(sampleQuestion));
        
        return "question-editor";
    }

    /**
     * Show the question editor for creating a new question with specific type and quiz
     */
    @GetMapping("/new")
    public String showNewQuestionEditor(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) Long quiz,
                                       Model model) {
        logger.info("Creating new question with type: {} for quiz: {}", type, quiz);

        // Create a new question based on type
        QuestionDto newQuestion = createNewQuestionByType(type);

        model.addAttribute("question", newQuestion);
        model.addAttribute("questionJson", formatQuestionAsJson(newQuestion));
        model.addAttribute("questionType", type != null ? type : "MC");
        model.addAttribute("quizId", quiz);

        return "question-editor";
    }

    /**
     * Show the question editor for editing an existing question
     */
    @GetMapping("/{questionId}")
    public String editQuestion(@PathVariable Long questionId, Model model, RedirectAttributes redirectAttributes) {

        logger.info("Loading question for editing: {}", questionId);
        
        try {
            // Use findQuestionById instead of getQuizzQuestionsForAuthor
            Question question = questionService.findQuestionById(questionId);
            
            if (question == null) {
                logger.warn("Question not found: {}", questionId);
                redirectAttributes.addFlashAttribute("errorMessage", "Question not found with ID: " + questionId);
                return "redirect:/question-editor";
            }
            
            QuestionDto questionDto = convertToDto(question);
            
            model.addAttribute("question", questionDto);
            model.addAttribute("questionJson", formatQuestionAsJson(questionDto));
            
            return "question-editor";
            
        } catch (Exception e) {
            logger.error("Error loading question: {}", questionId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading question: " + e.getMessage());
            return "redirect:/question-editor";
        }
    }

    /**
     * Save a question (create or update)
     */
    @PostMapping("/save")
    public String saveQuestion(@ModelAttribute QuestionDto questionDto, @RequestParam String questionType,
                             Model model, RedirectAttributes redirectAttributes) {

        logger.info("Saving question: {} (Type: {})", questionDto.getTitle(), questionType);
        
        try {
            // Validate required fields
            if (questionDto.getTitle() == null || questionDto.getTitle().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Question title is required");
                model.addAttribute("question", questionDto);
                model.addAttribute("questionJson", formatQuestionAsJson(questionDto));
                return "question-editor";
            }
            
            if (questionDto.getText() == null || questionDto.getText().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Question text is required");
                model.addAttribute("question", questionDto);
                model.addAttribute("questionJson", formatQuestionAsJson(questionDto));
                return "question-editor";
            }
            
            // Set default values if not provided
            if (questionDto.getAuthorName() == null || questionDto.getAuthorName().trim().isEmpty()) {
                questionDto.setAuthorName("Unknown Author");
            }
            
            if (questionDto.getChapter() == null || questionDto.getChapter().trim().isEmpty()) {
                questionDto.setChapter("General");
            }
            
            // Convert DTO to Entity and save
            Question question = convertToEntity(questionDto, questionType);
            Question savedQuestion = questionService.saveQuestion(question);
            
            logger.info("Question saved successfully with ID: {}", savedQuestion.getId());
            
            String action = (questionDto.getId() == null) ? "created" : "updated";
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question " + action + " successfully! ID: " + savedQuestion.getId());
            
            return "redirect:/question-editor/" + savedQuestion.getId();
            
        } catch (Exception e) {
            logger.error("Error saving question", e);
            model.addAttribute("errorMessage", "Error saving question: " + e.getMessage());
            model.addAttribute("question", questionDto);
            model.addAttribute("questionJson", formatQuestionAsJson(questionDto));
            return "question-editor";
        }
    }

    /**
     * Delete a question
     */
    @GetMapping("/delete/{questionId}")
    public String deleteQuestion(@PathVariable Long questionId, RedirectAttributes redirectAttributes) {

        logger.info("Deleting question: {}", questionId);
        
        try {
            Question question = questionService.findQuestionById(questionId);
            
            if (question == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Question not found with ID: " + questionId);
                return "redirect:/question-editor";
            }
            
            questionService.deleteQuestion(questionId);
            
            logger.info("Question deleted successfully: {}", questionId);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully!");
            
            return "redirect:/question-editor";
            
        } catch (Exception e) {
            logger.error("Error deleting question: {}", questionId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting question: " + e.getMessage());
            return "redirect:/question-editor";
        }
    }

    /**
     * Get question as JSON (for AJAX calls)
     */
    @GetMapping("/json/{questionId}")
    @ResponseBody
    public QuestionDto getQuestionAsJson(@PathVariable Long questionId) {

        logger.info("Getting question as JSON: {}", questionId);
        
        Question question = questionService.findQuestionById(questionId);
        if (question == null) {
            throw new ResourceNotFoundException("Question not found with ID: " + questionId);
        }
        
        return convertToDto(question);
    }

    /**
     * Create a sample question for the editor
     */
    private QuestionDto createSampleQuestion() {
        QuestionDto sample = new QuestionDto();
        sample.setTitle("Sample Database Question");
        sample.setText("Which SQL statement is used to retrieve data from a database?");
        sample.setChapter("SQL Basics");
        sample.setAuthorName("Sample Author");
        sample.setResponse1("SELECT");
        sample.setResponse2("INSERT");
        sample.setResponse3("UPDATE");
        sample.setResponse4("DELETE");
        sample.setWeightResponse1(100.0);
        sample.setWeightResponse2(0.0);
        sample.setWeightResponse3(0.0);
        sample.setWeightResponse4(0.0);
        return sample;
    }

    /**
     * Create a new question DTO based on the type
     */
    private QuestionDto createNewQuestionByType(String type) {
        QuestionDto question = new QuestionDto();

        if ("TF".equalsIgnoreCase(type)) {
            // True/False question
            question.setTitle("New True/False Question");
            question.setText("A primary key can contain NULL values.");
            question.setResponse1("True");
            question.setResponse2("False");
            question.setWeightTrue(0.0);
            question.setWeightFalse(100.0);
        } else {
            // Default to Multiple Choice
            question.setTitle("New Multiple Choice Question");
            question.setText("What does ACID stand for in database transactions?");
            question.setResponse1("Atomicity, Consistency, Isolation, Durability");
            question.setResponse2("Accuracy, Completeness, Integrity, Dependability");
            question.setResponse3("Access, Control, Implementation, Design");
            question.setResponse4("Automated, Consistent, Isolated, Distributed");
            question.setWeightResponse1(100.0);
            question.setWeightResponse2(0.0);
            question.setWeightResponse3(0.0);
            question.setWeightResponse4(0.0);
        }

        return question;
    }

    /**
     * Convert Question entity to DTO
     */
    private QuestionDto convertToDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setTitle(question.getTitle());
        dto.setText(question.getText());
        dto.setChapter(question.getChapter());
        // Note: Question entity doesn't have authorName field, so we'll use a default or get it from QuizAuthor
        dto.setAuthorName(question.getQuizAuthor() != null && question.getQuizAuthor().getAuthor() != null ?
                         question.getQuizAuthor().getAuthor().getName() : "Unknown Author");
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
        // Note: Question entity doesn't have row field, so we'll use crtNo
        dto.setRow(question.getCrtNo());
        return dto;
    }

    /**
     * Convert QuestionDto to Question entity
     */
    private Question convertToEntity(QuestionDto dto, String questionType) {
        Question question;

        // If this is an update (ID exists), fetch the existing question to preserve relationships
        if (dto.getId() != null) {
            question = questionService.findQuestionById(dto.getId());
            if (question == null) {
                // If question not found, create new one
                question = new Question();
                question.setId(dto.getId());
            }
            // Check if we need to update the author relationship
            String currentAuthorName = question.getQuizAuthor() != null && question.getQuizAuthor().getAuthor() != null ?
                                     question.getQuizAuthor().getAuthor().getName() : null;
            String newAuthorName = dto.getAuthorName() != null ? dto.getAuthorName().trim() : null;

            // If author name changed or there's no existing author, update the relationship
            if (!java.util.Objects.equals(currentAuthorName, newAuthorName)) {
                updateQuestionAuthorRelationship(question, newAuthorName);
            }
        } else {
            // Creating a new question
            question = new Question();
            // Set up the author relationship for new question
            updateQuestionAuthorRelationship(question, dto.getAuthorName());
        }
        
        question.setTitle(dto.getTitle());
        question.setText(dto.getText());
        question.setChapter(dto.getChapter());
        question.setCrtNo(dto.getRow() != null ? dto.getRow() : 1);

        if ("MC".equals(questionType)) {
            // Multiple Choice question
            question.setResponse1(dto.getResponse1());
            question.setResponse2(dto.getResponse2());
            question.setResponse3(dto.getResponse3());
            question.setResponse4(dto.getResponse4());
            question.setWeightResponse1(dto.getWeightResponse1() != null ? dto.getWeightResponse1() : 0.0);
            question.setWeightResponse2(dto.getWeightResponse2() != null ? dto.getWeightResponse2() : 0.0);
            question.setWeightResponse3(dto.getWeightResponse3() != null ? dto.getWeightResponse3() : 0.0);
            question.setWeightResponse4(dto.getWeightResponse4() != null ? dto.getWeightResponse4() : 0.0);
        } else if ("TF".equals(questionType)) {
            // True/False question
            question.setResponse1(dto.getResponse1()); // True or False
            question.setWeightTrue(dto.getWeightTrue() != null ? dto.getWeightTrue() : 0.0);
            question.setWeightFalse(dto.getWeightFalse() != null ? dto.getWeightFalse() : 0.0);
        }

        return question;
    }

    /**
     * Update the QuizAuthor relationship for a question
     */
    private void updateQuestionAuthorRelationship(Question question, String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            // No author specified, clear the relationship
            question.setQuizAuthor(null);
            return;
        }

        String trimmedAuthorName = authorName.trim();

        // Find existing author or create new one
        Author author = authorService.getAuthorByName(trimmedAuthorName);
        if (author == null) {
            // Create new author
            author = new Author();
            author.setName(trimmedAuthorName);
            author.setInitials(authorService.extractInitials(trimmedAuthorName));
            author = authorService.saveAuthor(author);
            logger.info("Created new author: {}", trimmedAuthorName);
        }

        // Check if question already has a QuizAuthor with the same author
        QuizAuthor existingQuizAuthor = question.getQuizAuthor();
        if (existingQuizAuthor != null &&
            existingQuizAuthor.getAuthor() != null &&
            existingQuizAuthor.getAuthor().getId().equals(author.getId())) {
            // Already has the correct author relationship, no need to change
            return;
        }

        // Create or update QuizAuthor relationship
        QuizAuthor quizAuthor = existingQuizAuthor != null ? existingQuizAuthor : new QuizAuthor();
        quizAuthor.setAuthor(author);
        // Note: Quiz relationship should be maintained if it exists

        question.setQuizAuthor(quizAuthor);
        logger.info("Updated question author relationship to: {}", trimmedAuthorName);
    }

    /**
     * Format question as JSON string for display
     */
    private String formatQuestionAsJson(QuestionDto question) {
        try {
            return objectMapper.writeValueAsString(question);
        } catch (Exception e) {
            logger.error("Error formatting question as JSON", e);
            return "{ \"error\": \"Could not format as JSON\" }";
        }
    }
}
