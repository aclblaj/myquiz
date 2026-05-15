package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final String ANSWER_TEXT_OPEN = "\" format=\"html\"><text><![CDATA[";
    private static final String ANSWER_TEXT_CLOSE = "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";

    private final QuestionRepository questionRepository;
    @Autowired
    public ExportService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public int writeToFile(String filePath, String category) {
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byCourse(category));
        return writeQuestionsToFile(filePath, category, questions);
    }

    @Transactional(readOnly = true)
    public String generateQuestionBankXml(String category, Long quizId) {
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byQuestionBankId(quizId));
        return buildXmlContent(category, questions);
    }

    @Transactional(readOnly = true)
    public String generateCourseXml(String category) {
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byCourse(category));
        return buildXmlContent(category, questions);
    }

    private int writeQuestionsToFile(String filePath, String category, List<Question> questions) {
        Path outputPath = Path.of(filePath);
        
        // Validate parent directory
        Path parentDir = outputPath.getParent();
        if (parentDir != null && !FileValidator.exists(parentDir)) {
            logger.atError().addArgument(parentDir).log("Parent directory does not exist: {}");
            return 0;
        }
        if (parentDir != null && !FileValidator.isDirectory(parentDir)) {
            logger.atError().addArgument(parentDir).log("Parent path is not a directory: {}");
            return 0;
        }
        
        int noOfQuestions = questions.size();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writer.write(buildXmlContent(category, questions));

            long cntMULTICHOICE = questions.stream().filter(q -> q.getType() == QuestionType.MULTICHOICE).count();
            long cntTRUEFALSE = questions.stream().filter(q -> q.getType() == QuestionType.TRUEFALSE).count();
            logger.atInfo().addArgument(noOfQuestions).addArgument(cntMULTICHOICE).addArgument(cntTRUEFALSE)
                  .addArgument(category)
                  .log("Number of exported questions: {} (MULTICHOICE: {}, TRUEFALSE: {}) for category/course '{}' ");
        } catch (IOException e) {
            logger.atError().setCause(e)
                  .addArgument(outputPath)
                  .log("Failed to write export file: {}");
        }
        
        return noOfQuestions;
    }

    private String buildXmlContent(String category, List<Question> questions) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<quiz>\n")
            .append("\t<!-- question: 0  -->\n")
            .append("\t<question type=\"category\">\n")
            .append("\t\t<category>\n")
            .append("\t\t\t<text>")
            .append(category)
            .append("</text>\n")
            .append("\t\t</category>\n")
            .append("\t\t<info format=\"moodle_auto_format\">\n")
            .append("\t\t\t<text>")
            .append(category)
            .append("</text>\n")
            .append("\t\t</info>\n")
            .append("\t\t<idnumber/>\n")
            .append("\t</question>\n");

        for (Question question : questions) {
            if (question.getType() == QuestionType.MULTICHOICE) {
                xml.append("\t<!-- question: ")
                    .append(getQuestionInitials(question))
                    .append("  -->\n")
                    .append(convertToXml(question))
                    .append("\n");
            }
        }

        xml.append("</quiz>");
        return xml.toString();
    }

    private String getQuestionInitials(Question question) {
        if (question == null || question.getQuestionBankAuthor() == null || question.getQuestionBankAuthor().getAuthor() == null
                || question.getQuestionBankAuthor().getAuthor().getInitials() == null
                || question.getQuestionBankAuthor().getAuthor().getInitials().isBlank()) {
            return "UNKNOWN";
        }
        return question.getQuestionBankAuthor().getAuthor().getInitials();
    }

    private String convertToXml(Question question) {
        StringBuilder xml = new StringBuilder(1024);
        Author author = question.getQuestionBankAuthor().getAuthor();
        String xmlQTitle = author.getInitials() + "-" + question.getTitle();
        if (null != question.getChapter()) {
            xmlQTitle = question.getChapter() + "-" + xmlQTitle;
        }
        
        xml.append("<question type=\"multichoice\"><name><text>")
           .append(xmlQTitle)
           .append("</text></name><questiontext format=\"html\"><text><![CDATA[")
           .append(question.getText())
           .append("]]></text></questiontext><generalfeedback format=\"html\"><text></text></generalfeedback><defaultgrade>1.0000000</defaultgrade><penalty>0.3333333</penalty><hidden>0</hidden><idnumber></idnumber><single>false</single><shuffleanswers>true</shuffleanswers><answernumbering>abc</answernumbering><showstandardinstruction>1</showstandardinstruction><correctfeedback format=\"html\"><text>Your answer is correct.</text></correctfeedback><partiallycorrectfeedback format=\"html\"><text>Your answer is partially correct.</text></partiallycorrectfeedback><incorrectfeedback format=\"html\"><text>Your answer is incorrect.</text></incorrectfeedback><shownumcorrect/><answer fraction=\"")
           .append(adjustNumberString(question.getWeightResponse1()))
           .append(ANSWER_TEXT_OPEN)
           .append(question.getResponse1())
           .append(ANSWER_TEXT_CLOSE)
           .append(adjustNumberString(question.getWeightResponse2()))
           .append(ANSWER_TEXT_OPEN)
           .append(question.getResponse2())
           .append(ANSWER_TEXT_CLOSE)
           .append(adjustNumberString(question.getWeightResponse3()))
           .append(ANSWER_TEXT_OPEN)
           .append(question.getResponse3())
           .append(ANSWER_TEXT_CLOSE)
           .append(adjustNumberString(question.getWeightResponse4()))
           .append(ANSWER_TEXT_OPEN)
           .append(question.getResponse4())
           .append("]]></text><feedback format=\"html\"><text></text></feedback></answer></question>");
        
        return xml.toString();
    }

    private String adjustNumberString(Double number) {
        if (number == null) {
            return "0";
        }
        if (number >= 100) {
            return "100";
        } else if (number >= 50) {
            return "50";
        } else if (number >= 33) {
            return "33.33333";
        } else if (number >= 25) {
            return "25";
        } else if (number <= -100) {
            return "-100";
        } else if (number <= -50) {
            return "-50";
        } else if (number <= -33) {
            return "-33.33333";
        } else if (number <= -25) {
            return "-25";
        }
        return "0";
    }
}
