package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.repositories.QuestionRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
@Service
public class ExportService {

    Logger logger = LoggerFactory.getLogger(ExportService.class.getName());

    QuestionRepository questionRepository;
    @Autowired
    public ExportService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }
    public int writeToFile(String filePath, String category) {
        int noOfQuestions = 0;
        int cntMC = 0;
        int cntTF = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<quiz>\n" +
                            "\t<!-- question: 0  -->\n" + "\t<question type=\"category\">\n" + "\t\t<category>\n" + "\t\t\t<text>" +
                            category + "</text>\n" + "\t\t</category>\n" + "\t\t<info format=\"moodle_auto_format\">\n" + "\t\t\t<text>" +
                            category + "</text>\n" + "\t\t</info>\n" + "\t\t<idnumber/>\n" + "\t</question>");
            writer.newLine(); // Write a new line

            List<Question> questions = questionRepository.findAll(Pageable.unpaged()).getContent();
            String xmlQ;
            for (Question question : questions) {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    xmlQ = convertToXml(question);
                    writer.write("\t<!-- question: " +
                                         question.getQuizAuthor().getAuthor().getInitials() + "  -->");
                    writer.newLine(); // Write a new line
                    writer.write(xmlQ);
                    writer.newLine(); // Write a new line
                    cntMC++;
                }
                if (question.getType() == QuestionType.TRUEFALSE) {
                    cntTF++;
                }
            }
            writer.write("</quiz>");
            noOfQuestions = questions.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.atInfo().addArgument(noOfQuestions).addArgument(cntMC).addArgument(cntTF)
              .log("Number of exported questions: {} (MC: {}, TF: {})");
        return noOfQuestions;
    }

    private String convertToXml(Question question) {
        String xml = "";
        String xmlQTitle = question.getQuizAuthor().getAuthor().getInitials() + "-" + question.getTitle();
        if (null != question.getCourse()) {
            xmlQTitle = question.getCourse() + "-" + xmlQTitle;
        }
        xml += "<question type=\"multichoice\"><name><text>";
        xml += xmlQTitle;
        xml += "</text></name><questiontext format=\"html\"><text><![CDATA[";
        xml += question.getText();
        xml += "]]></text></questiontext><generalfeedback format=\"html\"><text></text></generalfeedback><defaultgrade>1.0000000</defaultgrade><penalty>0.3333333</penalty><hidden>0</hidden><idnumber></idnumber><single>false</single><shuffleanswers>true</shuffleanswers><answernumbering>abc</answernumbering><showstandardinstruction>1</showstandardinstruction><correctfeedback format=\"html\"><text>Your answer is correct.</text></correctfeedback><partiallycorrectfeedback format=\"html\"><text>Your answer is partially correct.</text></partiallycorrectfeedback><incorrectfeedback format=\"html\"><text>Your answer is incorrect.</text></incorrectfeedback><shownumcorrect/><answer fraction=\"";
        xml += adjustNumberString(question.getWeightResponse1());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse1();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += adjustNumberString(question.getWeightResponse2());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse2();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += adjustNumberString(question.getWeightResponse3());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse3();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += adjustNumberString(question.getWeightResponse4());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse4();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer></question>";
        return xml;
    }

    private String adjustNumberString(Double number) {
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
