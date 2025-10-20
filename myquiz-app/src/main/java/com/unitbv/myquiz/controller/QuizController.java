package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquizapi.dto.QuestionDto;
import com.unitbv.myquizapi.dto.QuizDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping(value = "/quiz")
public class QuizController {

    private final QuizService quizService;

    private final AuthorService authorService;

    private final AuthorErrorService authorErrorService;

    @Autowired
    public QuizController(QuizService quizService,
                          AuthorService authorService,
                          AuthorErrorService authorErrorService) {
        this.quizService = quizService;
        this.authorService = authorService;
        this.authorErrorService = authorErrorService;
    }

    @GetMapping(value="/")
    public String listAllQuizzes(Model model) {

        List<QuizDto> quizDtos = quizService.getAllQuizzes();

        model.addAttribute("quizzes", quizDtos);
        return "quiz-list";
    }

    @GetMapping(value = "/{id}/delete")
    public String deleteCourse(
            @PathVariable(value = "id") Long id,
            RedirectAttributes redirectAttributes
    ) {
        quizService.deleteQuizById(id);
        authorService.deleteAuthorsWithoutQuiz();
        redirectAttributes.addFlashAttribute("message", "Quiz successfully deleted: " + id);
        return "redirect:/success";
    }

    @GetMapping(value = "/{id}/view")
    public String viewQuizAuthors(
            @PathVariable(value = "id") Long id,
            Model model
    ) {
//        QuizDto quizDto = quizService.getQuizById(id);
        String selectedCourse = quizService.getQuizById(id).getCourse();
        authorService.prepareFilterAuthorsModel(model, selectedCourse);
        return "author-list";
    }

    @GetMapping(value = "/{id}/errors")
    public String viewQuizErrors(
            @PathVariable(value = "id") Long id,
            Model model
    ) {
        QuizDto quizDto = quizService.getQuizById(id);
        authorErrorService.getAuthorErrorsModel(model, quizDto.getCourse(), null);
        return "error-list";
    }

    @GetMapping(value = "/{id}")
    public String viewQuiz(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found");
            return "redirect:/quiz/?error=Quiz not found";
        }
        model.addAttribute("quiz", quiz);
        return "quiz-details";
    }

    @GetMapping(value = "/edit/{id}")
    public String editQuiz(@PathVariable(value = "id") Long id, Model model) {
        try {
            QuizDto quiz = quizService.getQuizById(id);
            if (quiz == null) {
                return "redirect:/quiz/?error=Quiz not found";
            }
            model.addAttribute("quiz", quiz);
            return "quiz-editor";
        } catch (Exception e) {
            return "redirect:/quiz/?error=Error loading quiz for editing";
        }
    }

    @GetMapping(value = "/new")
    public String newQuiz(Model model) {
        QuizDto quiz = new QuizDto();
        model.addAttribute("quiz", quiz);
        return "quiz-editor";
    }

    @PostMapping(value = "/save")
    public String saveQuiz(@ModelAttribute QuizDto quiz, RedirectAttributes redirectAttributes) {
        try {
            if (quiz.getId() == null) {
                // Create new quiz
                Quiz newQuiz = quizService.createQuizz(quiz.getCourse(), quiz.getName(), quiz.getYear() != null ? quiz.getYear() : 2024);
                redirectAttributes.addFlashAttribute("message", "Quiz created successfully: " + newQuiz.getName());
                return "redirect:/quiz/" + newQuiz.getId();
            } else {
                // Update existing quiz
                Quiz updatedQuiz = quizService.updateQuiz(quiz.getId(), quiz.getCourse(), quiz.getName(), quiz.getYear() != null ? quiz.getYear() : 2024L);
                if (updatedQuiz != null) {
                    redirectAttributes.addFlashAttribute("message", "Quiz updated successfully: " + updatedQuiz.getName());
                    return "redirect:/quiz/" + updatedQuiz.getId();
                } else {
                    redirectAttributes.addFlashAttribute("error", "Quiz not found");
                    return "redirect:/quiz/";
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving quiz: " + e.getMessage());
            return "redirect:/quiz/";
        }
    }

    private QuestionDto toDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setTitle(question.getTitle());
        dto.setText(question.getText());
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
        dto.setChapter(question.getChapter());
        // Add other fields as needed
        return dto;
    }

    @GetMapping(value = "/{id}/export-mc")
    public void exportQuizToCsv(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
            return;
        }
        List<?> questionsRaw = quizService.getQuestionsByQuizId(id);
        response.setContentType("text/csv; charset=UTF-8");
        String safeCourse = quiz.getCourse() != null ? quiz.getCourse().replaceAll("[^a-zA-Z0-9]", "_") : "course";
        String safeQuizName = quiz.getName() != null ? quiz.getName().replaceAll("[^a-zA-Z0-9]", "_") : "quiz";
        String safeYear = quiz.getYear() != null ? quiz.getYear().toString() : "year";
        String filename = String.format("%s_%s_%s_MC.csv", safeCourse, safeQuizName, safeYear);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        String header = "Nr.,Curs,Titlu intrebare,Text intrebare,PR1,Raspuns 1,PR2,Raspuns 2,PR3,Raspuns 3,PR4,Raspuns 4, Feedback\n";
        ServletOutputStream out = response.getOutputStream();
        out.write("\uFEFF".getBytes("UTF-8")); // Write BOM for UTF-8
        out.write(header.getBytes("UTF-8"));
        int number = 1;
        for (Object q : questionsRaw) {
            Question question = (Question) q;
            if (question.getType() == null || !question.getType().equals(QuestionType.MULTICHOICE)) continue; // Only MC questions
            QuestionDto dto = toDto(question);
            String chapter = dto.getChapter() != null ? dto.getChapter() : "";
            String title = quiz.getName();
            String questionText = dto.getText() != null ? dto.getText() : "";
            String[] weights = new String[4];
            String[] responses = new String[4];
            weights[0] = dto.getWeightResponse1() != null ? dto.getWeightResponse1().toString() : "";
            weights[1] = dto.getWeightResponse2() != null ? dto.getWeightResponse2().toString() : "";
            weights[2] = dto.getWeightResponse3() != null ? dto.getWeightResponse3().toString() : "";
            weights[3] = dto.getWeightResponse4() != null ? dto.getWeightResponse4().toString() : "";
            responses[0] = dto.getResponse1() != null ? dto.getResponse1() : "";
            responses[1] = dto.getResponse2() != null ? dto.getResponse2() : "";
            responses[2] = dto.getResponse3() != null ? dto.getResponse3() : "";
            responses[3] = dto.getResponse4() != null ? dto.getResponse4() : "";
            StringBuilder line = new StringBuilder();
            line.append(number++).append(",");
            line.append(chapter).append(",");
            line.append(title).append(",");
            line.append("\"").append(questionText.replace("\"", "\"\"")).append("\"").append(",");
            for (int i = 0; i < 4; i++) {
                line.append(weights[i]).append(",");
                line.append("\"").append(responses[i].replace("\"", "\"\"")).append("\"");
                if (i < 3) line.append(",");
            }
            line.append("\n");
            out.write(line.toString().getBytes("UTF-8"));
        }
        out.flush();
        out.close();
    }

    @GetMapping(value = "/{id}/export-tf")
    public void exportQuizToCsvTF(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
            return;
        }
        List<?> questionsRaw = quizService.getQuestionsByQuizId(id);
        response.setContentType("text/csv; charset=UTF-8");
        String safeCourse = quiz.getCourse() != null ? quiz.getCourse().replaceAll("[^a-zA-Z0-9]", "_") : "course";
        String safeQuizName = quiz.getName() != null ? quiz.getName().replaceAll("[^a-zA-Z0-9]", "_") : "quiz";
        String safeYear = quiz.getYear() != null ? quiz.getYear().toString() : "year";
        String filename = String.format("%s_%s_%s_TF.csv", safeCourse, safeQuizName, safeYear);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        String header = "Nr.,Curs,Titlu,Text intrebare (afirmatie),TRUE,FALSE,Feedback\n";
        ServletOutputStream out = response.getOutputStream();
        out.write("\uFEFF".getBytes("UTF-8")); // Write BOM for UTF-8
        out.write(header.getBytes("UTF-8"));
        int number = 1;
        for (Object q : questionsRaw) {
            Question question = (Question) q;
            if (question.getType() == null || !question.getType().equals(QuestionType.TRUEFALSE)) continue; // Only TF questions
            QuestionDto dto = toDto(question);
            String chapter = dto.getChapter() != null ? dto.getChapter() : "";
            String title = dto.getTitle();
            String questionText = dto.getText() != null ? dto.getText() : "";
            String weightTrue = dto.getWeightTrue() != null ? dto.getWeightTrue().toString() : "";
            String weightFalse = dto.getWeightFalse() != null ? dto.getWeightFalse().toString() : "";
            StringBuilder line = new StringBuilder();
            line.append(number++).append(",");
            line.append(chapter).append(",");
            line.append(title).append(",");
            line.append("\"").append(questionText.replace("\"", "\"\"")).append("\"").append(",");
            line.append(weightTrue).append(",");
            line.append(weightFalse).append("");
            line.append("\n");
            out.write(line.toString().getBytes("UTF-8"));
        }
        out.flush();
        out.close();
    }
}
