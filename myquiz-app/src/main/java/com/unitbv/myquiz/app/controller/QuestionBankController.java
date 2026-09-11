package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankStatisticsDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.interfaces.QuestionBankApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.services.ExportService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import com.unitbv.myquiz.app.services.QuestionErrorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/question-banks"})
@CrossOrigin(origins = "${FRONTEND_URL}")
public class QuestionBankController implements QuestionBankApi {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankController.class);
    private static final String REGEX_SAFE = "[^a-zA-Z0-9]";
    private static final String DEFAULT_COURSE = "course";
    private static final String DEFAULT_QUESTIONBANK = "questionBank";
    private final QuestionBankService questionBankService;
    private final QuestionErrorService questionErrorService;
    private final ExportService exportService;

    // Remove @Autowired for constructor injection (Spring 4.3+ does this automatically)
    public QuestionBankController(QuestionBankService questionBankService, QuestionErrorService questionErrorService, ExportService exportService) {
        this.questionBankService = questionBankService;
        this.questionErrorService = questionErrorService;
        this.exportService = exportService;
    }

    @Override
    public ResponseEntity<List<QuestionBankDto>> getAllQuestionBanks() {
        log.atInfo().log("QuestionBankController.getAllQuestionBanks called");
        try {
            List<QuestionBankDto> questionBanks = questionBankService.getAllQuestionBanks();
            log.atInfo().addArgument(questionBanks.size()).log("Retrieved {} question banks");
            return ResponseEntity.ok(questionBanks);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error retrieving all question banks");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get QuestionBank by ID
     */
    @Override
    public ResponseEntity<QuestionBankDto> getQuestionBankById(@PathVariable Long id) {
        try {
            QuestionBankDto questionBank = questionBankService.getQuestionBankById(id);
            log.atInfo().addArgument(questionBank.getName()).log("Retrieved question bank: {}");
            return ResponseEntity.ok(questionBank);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(id).addArgument(e.getMessage()).log("Question bank not found with ID: {} - {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error retrieving question bank with ID: {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<QuestionBankExportDto> getQuestionBankExtendedById(@PathVariable Long id) {
        try {
            QuestionBankExportDto questionBank = questionBankService.getQuestionBankExtendedById(id);
            return ResponseEntity.ok(questionBank);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(id).addArgument(e.getMessage()).log("Extended question bank view not found with ID: {} - {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error retrieving extended question bank view with ID: {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new QuestionBank
     */
    @Override
    public ResponseEntity<QuestionBankDto> createQuestionBank(@Valid @RequestBody QuestionBankDto questionBankDto) {
        try {
            var questionBank = questionBankService.createQuestionBank(questionBankDto.getCourse(), questionBankDto.getName(), questionBankDto.getStudyYear());
            QuestionBankDto createdDto = questionBankService.getQuestionBankBasicById(questionBank.getId());
            return ResponseEntity.status(201).body(createdDto);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error creating question bank");
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update QuestionBank by ID
     */
    @Override
    public ResponseEntity<QuestionBankDto> updateQuestionBank(@PathVariable("id") Long id, @Valid @RequestBody QuestionBankDto questionBankDto) {
        try {
            var updatedQuestionBank = questionBankService.updateQuestionBank(id, questionBankDto.getCourse(), questionBankDto.getName(), questionBankDto.getStudyYear());
            if (updatedQuestionBank == null) return ResponseEntity.notFound().build();
            QuestionBankDto updatedDto = questionBankService.getQuestionBankBasicById(updatedQuestionBank.getId());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error updating question bank");
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete QuestionBank by ID
     */
    @Override
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable("id") Long id) {
        log.atInfo().addArgument(id).log("QuestionBankController.deleteQuestionBank called for question bank ID: {}");
        try {
            questionBankService.deleteQuestionBankById(id);
            log.atInfo().addArgument(id).log("Successfully deleted question bank with ID: {}");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // QuestionBank not found
            log.atWarn().addArgument(id).addArgument(e.getMessage()).log("Question bank not found with ID: {} - {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Other errors (database errors, transaction errors, etc.)
            log.atError().setCause(e).addArgument(id).log("Error deleting question bank with ID: {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get QuestionBanks by Course ID
     */
    @Override
    public ResponseEntity<List<QuestionBankDto>> getQuestionBanksByCourseId(@PathVariable("courseId") Long courseId) {
        try {
            List<QuestionBankDto> questionBanks = questionBankService.getQuestionBanksByCourseId(courseId);
            return ResponseEntity.ok(questionBanks);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error retrieving question banks for course ID: {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<QuestionBankDto>> getQuestionBanksByCourseName(@PathVariable("courseName") String courseName) {
        try {
            List<QuestionBankDto> questionBanks = questionBankService.getQuestionBanksByCourse(courseName);
            return ResponseEntity.ok(questionBanks);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseName).log("Error retrieving question banks for course name: {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildCsvLineMC(int number, QuestionDto dto, String chapter, String title, String questionText) {
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
        line.append(number).append(",");
        line.append(chapter).append(",");
        line.append(title).append(",");
        line.append("\"").append(questionText.replace("\"", "\"\"")).append("\"").append(",");
        for (int i = 0; i < 4; i++) {
            line.append(weights[i]).append(",");
            line.append("\"").append(responses[i].replace("\"", "\"\"")).append("\"");
            if (i < 3) line.append(",");
        }
        line.append("\n");
        return line.toString();
    }

    private String buildCsvLineTF(int number, QuestionDto dto, String chapter, String title, String questionText) {
        String weightTrue = dto.getWeightTrue() != null ? dto.getWeightTrue().toString() : "";
        String weightFalse = dto.getWeightFalse() != null ? dto.getWeightFalse().toString() : "";
        return number + "," + chapter + "," + title + "," + "\"" + questionText.replace("\"", "\"\"") + "\"" + "," + weightTrue + "," + weightFalse + "\n";
    }

    @Override
    public ResponseEntity<byte[]> exportQuestionBankToCsv(@PathVariable("id") Long id) {
        try {
            QuestionBankDto questionBank = questionBankService.getQuestionBankById(id);
            if (questionBank == null) {
                return ResponseEntity.notFound().build();
            }
            List<?> questionsRaw = questionBankService.getQuestionsByQuestionBankId(id);
            String safeCourse = questionBank.getCourse() != null ? questionBank.getCourse().replaceAll(REGEX_SAFE, "_") : DEFAULT_COURSE;
            String safeQuestionBankName = questionBank.getName() != null ? questionBank.getName().replaceAll(REGEX_SAFE, "_") : DEFAULT_QUESTIONBANK;
            String safeYear = questionBank.getStudyYear() != null ? questionBank.getStudyYear().getValue() : "year";
            String filename = String.format("%s_%s_%s_MC.csv", safeCourse, safeQuestionBankName, safeYear);
            StringBuilder csv = new StringBuilder("\uFEFF");
            csv.append("Nr.,Curs,Titlu intrebare,Text intrebare,PR1,Raspuns 1,PR2,Raspuns 2,PR3,Raspuns 3,PR4,Raspuns 4, Feedback\n");
            int number = 1;
            for (Object q : questionsRaw) {
                Question question = (Question) q;
                if (question.getType() == null || !question.getType().equals(QuestionType.MULTICHOICE)) continue;
                QuestionDto dto = toDto(question);
                String chapter = dto.getChapter() != null ? dto.getChapter() : "";
                String title = questionBank.getName();
                String questionText = dto.getText() != null ? dto.getText() : "";
                csv.append(buildCsvLineMC(number++, dto, chapter, title, questionText));
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename)
                    .body(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error exporting multiple-choice CSV for question bank {}" );
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<byte[]> exportQuestionBankToCsvTF(@PathVariable("id") Long id) {
        try {
            QuestionBankDto questionBank = questionBankService.getQuestionBankById(id);
            if (questionBank == null) {
                return ResponseEntity.notFound().build();
            }
            List<?> questionsRaw = questionBankService.getQuestionsByQuestionBankId(id);
            String safeCourse = questionBank.getCourse() != null ? questionBank.getCourse().replaceAll(REGEX_SAFE, "_") : DEFAULT_COURSE;
            String safeQuestionBankName = questionBank.getName() != null ? questionBank.getName().replaceAll(REGEX_SAFE, "_") : DEFAULT_QUESTIONBANK;
            String safeYear = questionBank.getStudyYear() != null ? questionBank.getStudyYear().getValue() : "year";
            String filename = String.format("%s_%s_%s_TF.csv", safeCourse, safeQuestionBankName, safeYear);
            StringBuilder csv = new StringBuilder("\uFEFF");
            csv.append("Nr.,Curs,Titlu,Text intrebare (afirmatie),TRUE,FALSE,Feedback\n");
            int number = 1;
            for (Object q : questionsRaw) {
                Question question = (Question) q;
                if (question.getType() == null || !question.getType().equals(QuestionType.TRUEFALSE)) continue;
                QuestionDto dto = toDto(question);
                String chapter = dto.getChapter() != null ? dto.getChapter() : "";
                String title = dto.getTitle();
                String questionText = dto.getText() != null ? dto.getText() : "";
                csv.append(buildCsvLineTF(number++, dto, chapter, title, questionText));
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename)
                    .body(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error exporting true-false CSV for question bank {}" );
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<byte[]> exportQuestionBankToXml(@PathVariable("id") Long id) {
        if (!hasExportXmlPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            QuestionBankDto questionBank = questionBankService.getQuestionBankById(id);
            if (questionBank == null) {
                return ResponseEntity.notFound().build();
            }

            String category = (questionBank.getCourse() != null ? questionBank.getCourse() : DEFAULT_COURSE) + "-" + (questionBank.getName() != null ? questionBank.getName() : DEFAULT_QUESTIONBANK);
            String xml = exportService.generateQuestionBankXml(category, id);

            String safeCourse = questionBank.getCourse() != null ? questionBank.getCourse().replaceAll(REGEX_SAFE, "_") : DEFAULT_COURSE;
            String safeQuestionBankName = questionBank.getName() != null ? questionBank.getName().replaceAll(REGEX_SAFE, "_") : DEFAULT_QUESTIONBANK;
            String safeYear = questionBank.getStudyYear() != null ? questionBank.getStudyYear().getValue() : "year";
            String filename = String.format("%s_%s_%s.xml", safeCourse, safeQuestionBankName, safeYear);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).header(HttpHeaders.CONTENT_DISPOSITION, ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename).body(
                    xml.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.atWarn().setCause(e).addArgument(id).log("Question bank not found for XML export: {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to export question bank {} to XML");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get QuestionBank statistics
     */
    @Override
    public ResponseEntity<QuestionBankStatisticsDto> getQuestionBankStatistics(@PathVariable Long id) {
        try {
            QuestionBankDto questionBank = questionBankService.getQuestionBankById(id);
            if (questionBank == null) return ResponseEntity.notFound().build();
            List<Question> questions = questionBankService.getQuestionsByQuestionBankId(id);
            Map<Long, QuestionBankStatisticsDto.AuthorStatsDto> statsMap = new java.util.HashMap<>();
            for (Question q : questions) {
                if (q.getQuestionBankAuthor() == null) continue;
                Long authorId = q.getQuestionBankAuthor().getId();
                String authorName = q.getQuestionBankAuthor().getAuthorName();
                QuestionBankStatisticsDto.AuthorStatsDto stat = statsMap.getOrDefault(authorId, new QuestionBankStatisticsDto.AuthorStatsDto());
                stat.setAuthorId(authorId);
                stat.setAuthorName(authorName);
                if (q.getType() == QuestionType.MULTICHOICE) stat.setMcCount(stat.getMcCount() + 1);
                if (q.getType() == QuestionType.TRUEFALSE) stat.setTfCount(stat.getTfCount() + 1);
                statsMap.put(authorId, stat);
            }
            // Count errors per author
            for (QuestionBankStatisticsDto.AuthorStatsDto stat : statsMap.values()) {
                int errorCount = questionErrorService.countErrorsByAuthorAndQuestionBank(stat.getAuthorId(), questionBank.getId());
                stat.setErrorCount(errorCount);
            }
            QuestionBankStatisticsDto dto = new QuestionBankStatisticsDto();
            dto.setQuestionBank(questionBank);
            dto.setAuthorStats(new java.util.ArrayList<>(statsMap.values()));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error fetching statistics for question bank {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Filter questionBanks
     */
    @Override
    public ResponseEntity<QuestionBankFilterResponseDto> filterQuestionBanks(@Valid @RequestBody QuestionBankFilterRequestDto filterInput) {
        log.atInfo().addArgument(filterInput).log("QuestionBankController.filterQuestionBanks called with input: {}");
        try {
            QuestionBankFilterResponseDto result = questionBankService.filterQuestionBanks(filterInput);
            log.atInfo().addArgument(result.getQuestionBanks().size()).log("Filtered question banks count: {}");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error filtering question banks");
            return ResponseEntity.internalServerError().build();
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
        return dto;
    }

    private boolean hasExportXmlPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(authority -> ControllerSettings.PERMISSION_EXPORT_XML.equals(authority.getAuthority()));
    }
}


