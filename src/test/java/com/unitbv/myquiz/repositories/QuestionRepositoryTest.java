package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.AuthorErrors;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class QuestionRepositoryTest {

    public static final String CATEGORY = "2024-BD-Q1-01";
    public static final String XLSX_DIR_WITH_FILES = "C:\\work\\_mi\\2024-BD\\inpQ1\\";
    public static final String OUTPUT_FILE = "C:\\work\\_mi\\2024-BD\\inpQ1.xml";
    Logger logger = Logger.getLogger(QuestionRepositoryTest.class.getName());

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    AuthorErrorsRepository authorErrorsRepository;

    String authorName;
    String initials;

    private static String getAuthorName(String filePath) {
        String authorPlus = filePath.substring(filePath.indexOf("inpQ1") + 6);
        String authorName = authorPlus.substring(0, authorPlus.indexOf("_"));
        return authorName;
    }

    private static String extractInitials(String authorName) {
        String initials = "";
        if (authorName.length() > 0) {
            String[] split = authorName.split(" ");
            for (String s : split) {
                initials += s.charAt(0);
            }
        }
        return initials;
    }

    private static String cleanSpecialChars(String text) {
        text = text.replace("ș", "s");
        text = text.replace("ț", "t");
        text = text.replace("ţ", "t");
        text = text.replace("ă", "a");
        text = text.replace("â", "a");
        text = text.replace("î", "i");
        text = text.replace("Ș", "S");
        text = text.replace("Ț", "T");
        text = text.replace("Ă", "A");
        text = text.replace("Â", "A");
        text = text.replace("Î", "I");
        text = text.replace("–", "-");
        text = text.replace("„", "\"");
        text = text.replace("”", "\"");
        text = text.replace("’", "'");
        text = text.replace("…", "...");
        text = text.replace("–", "-");
        text = text.replace("—", "-");
        text = text.replace("\n", " ");
        text = text.replace("\r", " ");
        text = text.replace("\t", " ");
        text = text.replace("&", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.trim();
        text = text.replace("A.", "");
        text = text.replace("B.", "");
        text = text.replace("C.", "");
        text = text.replace("D.", "");
        text = text.replace("a.", "");
        text = text.replace("b.", "");
        text = text.replace("c.", "");
        text = text.replace("d.", "");
        text = text.replace("1.", "");
        text = text.replace("2.", "");
        text = text.replace("3.", "");
        text = text.replace("4.", "");
        text = text.replace("A)", "");
        text = text.replace("B)", "");
        text = text.replace("C)", "");
        text = text.replace("D)", "");
        text = text.replace("a)", "");
        text = text.replace("b)", "");
        text = text.replace("c)", "");
        text = text.replace("d)", "");
        text = text.replace("1)", "");
        text = text.replace("2)", "");
        text = text.replace("3)", "");
        text = text.replace("4)", "");
        return text;
    }

    private static String convertFromUTF8(String text) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(text.getBytes(), 0, text.length() - 1);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();

        String result = text;
        if (encoding != null && encoding.equalsIgnoreCase("UTF-8")) {
            result = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }
        return result;
    }

    @Test
    void parseExcelFiles() {
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: " + result);
        assertNotEquals(0, result);
    }

    private int parseExcelFilesFromFolder(File folder, int noFilesInput) {
        int noFiles = noFilesInput;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    noFiles = parseExcelFilesFromFolder(file, noFiles);
                }
            }
        } else if (folder.isFile() && folder.getName().endsWith(".xlsx")) {
            authorName = getAuthorName(folder.getAbsolutePath());
            initials = extractInitials(authorName);
            readAndParseFirstSheetFromExcelFile(folder.getAbsolutePath());
            noFiles++;
        } else {
            logger.info("Not readable target file: " + folder.getAbsolutePath());
            addAuthorError("eroare template - fisierul nu are tipul cerut (excel)", -1);
        }
        return noFiles;
    }

    private void readAndParseFirstSheetFromExcelFile(String filePath) {
        logger.info("Parse excel file: " + filePath);


        try (FileInputStream fileInputStream = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet

            if (sheet.getLastRowNum() < 15) {
                addAuthorError("tema incompleta - mai putin de 15 intrebari multiple choice", 0);
                logger.info("tema incompleta: sheet.getLastRowNum()<15");
                return;
            }

            // Iterate over rows
            for (Row row : sheet) {
                int currentRowNumber = row.getRowNum();

                int noNotNull = countNotNullValues(row);
                if (noNotNull < 11) {
                    addAuthorError("eroare - valori lipsa, mai putin de 11 ", currentRowNumber);
                    break;
                }

                if (row.getCell(3) != null) {
                    if (row.getCell(3).getCellType() == CellType.STRING && (row.getCell(3).getStringCellValue().contains("PR1") || row.getCell(3).getStringCellValue().contains("Punctaj"))) {
                        continue; // skip header line
                    }
                }

                boolean skipDueToError = false;

                double cellNrCrtDouble = 0.0;
                String titlu = "";
                String text = "";
                double PR1 = 0.0;
                String Raspuns1;
                double PR2 = 0.0;
                String Raspuns2;
                double PR3 = 0.0;
                String Raspuns3;
                double PR4 = 0.0;
                String Raspuns4;

                Cell cellNrCrt = row.getCell(0);
                cellNrCrtDouble = convertCellToDouble(cellNrCrt);

                Cell cellTitlu = row.getCell(1);
                if (cellTitlu == null) {
                    addAuthorError("eroare template - titlul este null", currentRowNumber);
                    skipDueToError = true;
                } else {
                    if (cellTitlu.getCellType() == CellType.NUMERIC) {
                        addAuthorError("eroare template - titlu nu este string", currentRowNumber);
                        break;
                    }
                    titlu = cellTitlu.getStringCellValue();
                    if (titlu.length() < 2) {
                        addAuthorError("eroare template - titlu nu este introdus", currentRowNumber);
                        skipDueToError = true;
                    } else {
                        if (titlu.contains("Kapazität Null") || titlu.contains("Kommunikationsmuster") || titlu.contains("ICMP") || titlu.contains("Roluri 2PC") || titlu.contains(
                                "IP header") || titlu.contains("c02_scheduler_functie")) {
                            addAuthorError("eroare template - intrebare sablon - de sters: ", currentRowNumber);
                            break;
                        }
                    }
                }

                Cell cellText = row.getCell(2);
                if (cellTitlu == null) {
                    addAuthorError("eroare template - test intrebare este null", currentRowNumber);
                    skipDueToError = true;
                } else {
                    text = cellText.getStringCellValue();
                    if (text.length() == 0) {
                        addAuthorError("eroare template - text nu este introdus", currentRowNumber);
                        skipDueToError = true;
                    }
                }

                Cell cellPR1 = row.getCell(3);
                PR1 = convertCellToDouble(cellPR1);

                Cell cellRaspuns1 = row.getCell(4);
                Raspuns1 = getValueAsString(cellRaspuns1);

                Cell cellPR2 = row.getCell(5);
                PR2 = convertCellToDouble(cellPR2);

                Cell cellRaspuns2 = row.getCell(6);
                Raspuns2 = getValueAsString(cellRaspuns2);

                Cell cellPR3 = row.getCell(7);
                PR3 = convertCellToDouble(cellPR3);

                Cell cellRaspuns3 = row.getCell(8);
                Raspuns3 = getValueAsString(cellRaspuns3);

                Cell cellPR4 = row.getCell(9);
                PR4 = convertCellToDouble(cellPR4);

                Cell cellRaspuns4 = row.getCell(10);
                Raspuns4 = getValueAsString(cellRaspuns4);

                double total = PR1 + PR2 + PR3 + PR4;
                if (PR1 == 25 && total != 100) {
                    addAuthorError("eroare template - suma punctajelor 4/4 este incorecta", currentRowNumber);
                    skipDueToError = true;
                }
                if (((int) PR1 == 33 || (int) PR2 == 33 || (int) PR3 == 33 || (int) PR4 == 33) && total > 1) {
                    addAuthorError("eroare template - suma punctajelor 3/4 este incorecta", currentRowNumber);
                    break;
                }
                if ((PR1 == 50 || PR2 == 50 || PR3 == 50 || PR4 == 50) && total != 0) {
                    addAuthorError("eroare template - suma punctajelor 2/4 este incorecta", currentRowNumber);
                    skipDueToError = true;
                }
                if (((PR1 == 100 || PR2 == 100 || PR3 == 100 || PR4 == 100) && (total != 100 || total != -200))) {
                    addAuthorError("eroare template - suma punctajelor 1/4 este incorecta", currentRowNumber);
                    skipDueToError = true;
                }

                if (!skipDueToError) {
                    titlu = cleanAndConvertString(titlu);
                    text = cleanAndConvertString(text);
                    Raspuns1 = cleanAndConvertString(Raspuns1);
                    Raspuns2 = cleanAndConvertString(Raspuns2);
                    Raspuns3 = cleanAndConvertString(Raspuns3);
                    Raspuns4 = cleanAndConvertString(Raspuns4);

                    if (!Raspuns1.isEmpty() && !Raspuns2.isEmpty() && !Raspuns3.isEmpty() && !Raspuns4.isEmpty()) {
                        Question question = initQuestion(cellNrCrtDouble, titlu, text, PR1, Raspuns1, PR2, Raspuns2, PR3, Raspuns3, PR4, Raspuns4);
                        if (checkAllAnswersForDuplicates(question)) {
                            addAuthorError("raspuns dublat - reformulare raspunsuri/intrebare ", currentRowNumber);
                        }
//                       else if (checkAllTitlesForDuplicates(titlu)) {
//                            addAuthorError("titlu dublat - reformulare titlu intrebare ", currentRowNumber);
//                        };
                        else if (!addQuestion(question)) {
                            addAuthorError("eroare salvare - caractere nepermise", currentRowNumber);
                        }
                    } else {
                        addAuthorError("eroare template - cel putin un raspuns lipsa", currentRowNumber);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double convertCellToDouble(Cell cell) {
        double result = 0.0;
        try {
            if (cell == null) {
                addAuthorError("lipsa punctaj", -1);
            } else {
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    result = Double.parseDouble(cell.getStringCellValue());
                } else if (cellType == CellType.NUMERIC) {
                    result = cell.getNumericCellValue();
                } else {
                    addAuthorError("coloana punctaj nu e string sau numeric", cell.getRowIndex());
                }
            }
        } catch (Exception e) {
            addAuthorError("tip de date eronat", cell.getRowIndex());
        }
        return result;
    }

    private String cleanAndConvertString(String text) {
        if (text == null) {
            return "";
        }
        text = cleanSpecialChars(text);
        String result = convertFromUTF8(text);
        return result;
    }

    private int countNotNullValues(Row row) {
        int count = 0;
        for (Cell cell : row) {
            if (cell != null) {
                if (cell.getCellType() == CellType.STRING) {
                    if (cell.getStringCellValue().length() > 0) {
                        count++;
                    }
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    count++;
                }
            }
        }
        return count;
    }

    private String getValueAsString(Cell cell) {
        String result = "";
        try {
            if (cell != null) {
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    result = cell.getStringCellValue();
                } else if (cellType == CellType.NUMERIC) {
                    result = String.valueOf(cell.getNumericCellValue());
                }
            }
            if (result.isEmpty()) {
                addAuthorError("raspuns lipsa", cell.getRowIndex());
            }
        } catch (Exception e) {
            addAuthorError("tip de date valoare eronata", cell.getRowIndex());
        }
        return result;
    }

    private Question initQuestion(double cellNrCrtDouble, String titlu, String text, double pr1, String raspuns1, double pr2, String raspuns2, double pr3, String raspuns3, double pr4, String raspuns4) {
        Question question = new Question();
        question.setCrtNo((int) cellNrCrtDouble);
        question.setTitle(titlu);
        question.setText(text);
        question.setType(QuestionType.MULTICHOICE);
        question.setWeightResponse1(pr1);
        question.setResponse1(raspuns1);
        question.setWeightResponse2(pr2);
        question.setResponse2(raspuns2);
        question.setWeightResponse3(pr3);
        question.setResponse3(raspuns3);
        question.setWeightResponse4(pr4);
        question.setResponse4(raspuns4);
        question.setAuthor(authorName);
        question.setInitiale(initials);
        questionRepository.save(question);
        return question;
    }
    private boolean addQuestion(Question question) {
        try {
            questionRepository.save(question);
            return true;
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
            System.out.println("question: " + question.toString());
            return false;
        }
    }

    private boolean checkAllTitlesForDuplicates(String titlu) {
        List<String> allTitles = putAllTitlesToList();
        if (titlu != null && allTitles.contains(titlu.toLowerCase())) {
            return true;
        }
        return false;
    }

    private List<String> putAllTitlesToList() {
        List<String> allTitles = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            allTitles.add(question.getTitle().toLowerCase());
        }
        return allTitles;
    }

    private boolean checkAllAnswersForDuplicates(Question question) {
        List<String> allQuestionsAnswers = putAllQuestionsToList();
        if (question.getResponse1() != null && allQuestionsAnswers.contains(question.getResponse1().toLowerCase())) {
            return true;
        }
        if (question.getResponse2() != null && allQuestionsAnswers.contains(question.getResponse2().toLowerCase())) {
            return true;
        }
        if (question.getResponse3() != null && allQuestionsAnswers.contains(question.getResponse3().toLowerCase())) {
            return true;
        }
        if (question.getResponse4() != null && allQuestionsAnswers.contains(question.getResponse4().toLowerCase())) {
            return true;
        }
        return false;
    }

    private List<String> putAllQuestionsToList() {
        List<String> allAnswers = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            if (question.getResponse1() != null) allAnswers.add(question.getResponse1().toLowerCase());
            if (question.getResponse2() != null) allAnswers.add(question.getResponse2().toLowerCase());
            if (question.getResponse3() != null) allAnswers.add(question.getResponse3().toLowerCase());
            if (question.getResponse4() != null) allAnswers.add(question.getResponse4().toLowerCase());
        }
        return allAnswers;
    }

    private void addAuthorError(String description, int rowNumber) {
        AuthorErrors authorErrors = new AuthorErrors();
        authorErrors.setName(authorName);
        authorErrors.setInitials(initials);
        authorErrors.setDescription(description);
        authorErrors.setRowNumber(rowNumber);
        authorErrorsRepository.save(authorErrors);
    }

    @Test
    void writeToFile() {
        String filePath = OUTPUT_FILE;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<quiz>\n" + "\t<!-- question: 0  -->\n" + "\t<question type=\"category\">\n" + "\t\t<category>\n" + "\t\t\t<text>" + CATEGORY + "</text>\n" + "\t\t</category>\n" + "\t\t<info format=\"moodle_auto_format\">\n" + "\t\t\t<text>" + CATEGORY + "</text>\n" + "\t\t</info>\n" + "\t\t<idnumber/>\n" + "\t</question>");
            writer.newLine(); // Write a new line

            List<Question> questions = questionRepository.findAll(Pageable.unpaged()).getContent();
            String xmlQ;
            for (Question question : questions) {
                xmlQ = convertToXml(question);
                writer.write("\t<!-- question: " + question.getInitiale() + "  -->");
                writer.newLine(); // Write a new line
                writer.write(xmlQ);
                writer.newLine(); // Write a new line
            }
            writer.write("</quiz>");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotEquals(0, 1);
    }

    private String convertToXml(Question question) {
        String xml = "";
        xml += "<question type=\"multichoice\"><name><text>";
        xml += question.getInitiale() + "-" + question.getTitle();
        xml += "</text></name><questiontext format=\"html\"><text><![CDATA[";
        xml += question.getText();
        xml += "]]></text></questiontext><generalfeedback format=\"html\"><text></text></generalfeedback><defaultgrade>1.0000000</defaultgrade><penalty>0.3333333</penalty><hidden>0</hidden><idnumber></idnumber><single>false</single><shuffleanswers>true</shuffleanswers><answernumbering>abc</answernumbering><showstandardinstruction>1</showstandardinstruction><correctfeedback format=\"html\"><text>Your answer is correct.</text></correctfeedback><partiallycorrectfeedback format=\"html\"><text>Your answer is partially correct.</text></partiallycorrectfeedback><incorrectfeedback format=\"html\"><text>Your answer is incorrect.</text></incorrectfeedback><shownumcorrect/><answer fraction=\"";
        xml += convertNumberToString(question.getWeightResponse1());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse1();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += convertNumberToString(question.getWeightResponse2());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse2();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += convertNumberToString(question.getWeightResponse3());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse3();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer><answer fraction=\"";
        xml += convertNumberToString(question.getWeightResponse4());
        xml += "\" format=\"html\"><text><![CDATA[";
        xml += question.getResponse4();
        xml += "]]></text><feedback format=\"html\"><text></text></feedback></answer></question>";
        return xml;
    }

    private String convertNumberToString(Double number) {
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
        return "";
    }
}
