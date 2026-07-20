package com.unitbv.myquiz.app.upload.domain.parsing;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.types.QuestionType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class XmlQuestionParser {

    public List<QuestionDto> parseXmlQuestions(MultipartFile xml) {
        try (InputStream inputStream = xml.getInputStream()) {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList questionNodes = document.getElementsByTagName("question");
            ArrayList<QuestionDto> parsed = new ArrayList<>();
            for (int i = 0; i < questionNodes.getLength(); i++) {
                Node node = questionNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element questionElement = (Element) node;
                QuestionType xmlQuestionType = resolveXmlQuestionType(questionElement.getAttribute("type"));
                if (xmlQuestionType == null) {
                    continue;
                }

                ParsedExportedName parsedName = parseExportedName(readNestedText(questionElement, "name", "text"));
                String text = readNestedText(questionElement, "questiontext", "text");

                NodeList answerNodes = questionElement.getElementsByTagName("answer");
                String[] responses = new String[4];
                Double[] weights = new Double[] {0d, 0d, 0d, 0d};
                for (int a = 0; a < Math.min(4, answerNodes.getLength()); a++) {
                    Element answerElement = (Element) answerNodes.item(a);
                    responses[a] = readText(answerElement, "text");
                    weights[a] = parseFraction(answerElement.getAttribute("fraction"));
                }

                QuestionDto dto = new QuestionDto();
                dto.setTitle(parsedName.title());
                dto.setChapter(parsedName.chapter());
                dto.setAuthor(AuthorInfo.builder()
                        .name(parsedName.initials())
                        .initials(parsedName.initials())
                        .build());
                dto.setText(text);

                if (QuestionType.TRUEFALSE.equals(xmlQuestionType)) {
                    dto.setType(QuestionType.TRUEFALSE);
                    applyTrueFalseAnswers(dto, responses, weights);
                } else {
                    dto.setType(QuestionType.MULTICHOICE);
                    dto.setResponse1(responses[0]);
                    dto.setResponse2(responses[1]);
                    dto.setResponse3(responses[2]);
                    dto.setResponse4(responses[3]);
                    dto.setWeightResponse1(weights[0]);
                    dto.setWeightResponse2(weights[1]);
                    dto.setWeightResponse3(weights[2]);
                    dto.setWeightResponse4(weights[3]);
                }

                parsed.add(dto);
            }
            return parsed;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException("Invalid XML format: " + e.getMessage(), e);
        }
    }

    private void applyTrueFalseAnswers(QuestionDto dto, String[] responses, Double[] weights) {
        Double trueWeight = null;
        Double falseWeight = null;
        for (int i = 0; i < responses.length; i++) {
            String response = responses[i];
            if (response == null) {
                continue;
            }
            String normalized = response.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized) || "adevarat".equals(normalized)) {
                trueWeight = weights[i];
            } else if ("false".equals(normalized) || "fals".equals(normalized)) {
                falseWeight = weights[i];
            }
        }

        if (trueWeight == null && weights.length > 0) {
            trueWeight = weights[0];
        }
        if (falseWeight == null && weights.length > 1) {
            falseWeight = weights[1];
        }
        dto.setWeightTrue(trueWeight != null ? trueWeight : 0d);
        dto.setWeightFalse(falseWeight != null ? falseWeight : 0d);
        dto.setResponse1("True");
    }

    private QuestionType resolveXmlQuestionType(String xmlType) {
        if (xmlType == null || xmlType.isBlank()) {
            return null;
        }
        for (QuestionType type : new QuestionType[] {QuestionType.MULTICHOICE, QuestionType.TRUEFALSE}) {
            if (type.name().equalsIgnoreCase(xmlType.trim())) {
                return type;
            }
        }
        return null;
    }

    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private String readNestedText(Element root, String parentTag, String childTag) {
        NodeList parentNodes = root.getElementsByTagName(parentTag);
        if (parentNodes.getLength() == 0) {
            return null;
        }
        return readText((Element) parentNodes.item(0), childTag);
    }

    private String readText(Element root, String tagName) {
        NodeList nodes = root.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value != null ? value.trim() : null;
    }

    private Double parseFraction(String fraction) {
        try {
            return fraction == null || fraction.isBlank() ? 0d : Double.parseDouble(fraction.trim());
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private ParsedExportedName parseExportedName(String exportedName) {
        if (exportedName == null || exportedName.isBlank()) {
            return new ParsedExportedName(null, null, null);
        }

        String cleaned = exportedName.trim();
        String[] parts = cleaned.split("-", 3);
        if (parts.length == 3 && looksLikeInitials(parts[1])) {
            return new ParsedExportedName(parts[1].trim(), parts[0].trim(), parts[2].trim());
        }
        if (parts.length >= 2 && looksLikeInitials(parts[0])) {
            String title = cleaned.substring(parts[0].length() + 1).trim();
            return new ParsedExportedName(parts[0].trim(), null, title);
        }
        return new ParsedExportedName(null, null, cleaned);
    }

    private boolean looksLikeInitials(String value) {
        return value != null && value.trim().matches("[A-Za-z]{1,10}");
    }

    private record ParsedExportedName(String initials, String chapter, String title) {
    }
}
