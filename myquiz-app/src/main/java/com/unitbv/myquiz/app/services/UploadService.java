package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.ArchiveFolderItemDto;
import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveImportDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.util.FileValidator;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Service for handling file upload operations.
 * Implements business logic for Excel and Archive uploads as per upload-sd.md specifications.
 */
@Service
public class UploadService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";

    private final QuestionService questionService;
    private final AuthorService authorService;
    private final FileService fileService;
    private final CourseService courseService;
    private final ArchiveImportService archiveImportService;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionRepository questionRepository;

    public UploadService(QuestionService questionService, AuthorService authorService, FileService fileService, CourseService courseService,
                         ArchiveImportService archiveImportService, QuestionBankAuthorRepository questionBankAuthorRepository,
                         QuestionRepository questionRepository) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.fileService = fileService;
        this.courseService = courseService;
        this.archiveImportService = archiveImportService;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public String processXmlUpload(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear) {
        validateXmlUploadInputs(xml, courseId, questionBankName, studyYear);

        CourseDto courseDto = findCourseById(courseId);
        if (courseDto == null) {
            throw new IllegalArgumentException("Course not found for ID: " + courseId);
        }

        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(questionBankName);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(courseDto.getCourse()));
        questionBank.setStudyYear(studyYear);
        final QuestionBank persistedQuestionBank = questionService.saveQuestionBank(questionBank);

        // Cache existing question keys for the selected course to avoid duplicate imports.
        Set<String> courseQuestionCache = buildCourseQuestionCache(courseDto.getCourse());
        Map<String, Author> authorsByInitials = loadAuthorCacheByInitials();
        Map<Long, QuestionBankAuthor> relationByAuthorId = new HashMap<>();

        List<QuestionDto> parsedQuestions = parseXmlQuestions(xml);
        int imported = 0;
        int skippedDuplicates = 0;
        int invalidQuestions = 0;
        int crtNo = 1;

        for (QuestionDto parsed : parsedQuestions) {
            String key = buildQuestionKey(parsed.getTitle(), parsed.getText());
            if (key == null) {
                invalidQuestions++;
                continue;
            }
            if (courseQuestionCache.contains(key)) {
                skippedDuplicates++;
                continue;
            }

            Author author = resolveAuthor(parsed.getAuthorName(), authorsByInitials);
            QuestionBankAuthor relation = relationByAuthorId.computeIfAbsent(author.getId(), authorId -> {
                QuestionBankAuthor qba = new QuestionBankAuthor();
                qba.setAuthor(author);
                qba.setQuestionBank(persistedQuestionBank);
                qba.setSource(safeArchiveName(xml.getOriginalFilename()));
                return questionBankAuthorRepository.save(qba);
            });

            Question question = new Question();
            question.setCrtNo(crtNo++);
            question.setType(parsed.getType());
            question.setChapter(parsed.getChapter());
            question.setTitle(parsed.getTitle());
            question.setText(parsed.getText());
            question.setResponse1(parsed.getResponse1());
            question.setResponse2(parsed.getResponse2());
            question.setResponse3(parsed.getResponse3());
            question.setResponse4(parsed.getResponse4());
            question.setWeightResponse1(parsed.getWeightResponse1());
            question.setWeightResponse2(parsed.getWeightResponse2());
            question.setWeightResponse3(parsed.getWeightResponse3());
            question.setWeightResponse4(parsed.getWeightResponse4());
            question.setWeightTrue(parsed.getWeightTrue());
            question.setWeightFalse(parsed.getWeightFalse());
            question.setQuestionBankAuthor(relation);
            questionService.saveQuestion(question);

            courseQuestionCache.add(key);
            imported++;
        }

        logger.atInfo().addArgument(imported).addArgument(skippedDuplicates).addArgument(invalidQuestions).addArgument(persistedQuestionBank.getId())
                .log("XML import completed: imported={}, duplicatesSkipped={}, invalid={}, questionBankId={}");

        return "XML import completed. Imported " + imported + " question(s), skipped " + skippedDuplicates + " duplicate(s), ignored " +
                invalidQuestions + " invalid question(s).";
    }

    /**
     * Process a folder selection represented by multiple uploaded archive files.
     * Each archive is processed independently with generated unique course/QuestionBank names.
     * Files with sizes already processed before are skipped.
     */
    public ArchiveFolderUploadResultDto processArchiveFolderUpload(MultipartFile[] archives, StudyYear studyYear) {
        ArchiveFolderUploadResultDto result = new ArchiveFolderUploadResultDto();
        if (archives == null || archives.length == 0) {
            logger.atWarn().log("processArchiveFolderUpload called with no archives");
            return result;
        }

        logger.atInfo().addArgument(archives.length).addArgument(studyYear).log("Starting folder archive upload processing: files={}, studyYear={}");

        List<MultipartFile> inputArchives = Arrays.stream(archives).filter(file -> file != null && !file.isEmpty()).toList();

        if (inputArchives.isEmpty()) {
            logger.atWarn().log("No non-empty archives found in folder upload request");
            return result;
        }

        result.setTotalArchives(inputArchives.size());

        logger.atInfo().addArgument(inputArchives.size()).addArgument(studyYear).log("Processing {} archive(s) using studyYear {} for generated QuestionBanks");
        int index = 0;
        for (MultipartFile archive : inputArchives) {
            index++;
            ArchiveFolderItemDto item = new ArchiveFolderItemDto();
            item.setIndex(index);
            item.setTotal(inputArchives.size());
            item.setArchiveName(safeArchiveName(archive.getOriginalFilename()));

            logger.atInfo().addArgument(index).addArgument(inputArchives.size()).addArgument(item.getArchiveName()).log("Folder archive item {}/{}: {}");

            if (!isZipArchive(archive)) {
                item.setStatus("SKIPPED");
                item.setMessage("Skipped non-archive file");
                result.setSkippedArchives(result.getSkippedArchives() + 1);
                logger.atInfo().addArgument(item.getArchiveName()).log("Skipping folder item '{}' because it is not a ZIP archive");
                result.getItems().add(item);
                continue;
            }

            long fileSize = archive.getSize();
            if (archiveImportService != null && archiveImportService.existsBySize(fileSize)) {
                item.setStatus("SKIPPED");
                item.setMessage("Skipped archive because another processed archive has the same size");
                result.setSkippedArchives(result.getSkippedArchives() + 1);
                logger.atInfo().addArgument(item.getArchiveName()).addArgument(fileSize).log("Skipping folder item '{}' because an archive with size {} was already processed");
                result.getItems().add(item);
                continue;
            }

            String uniqueCourseName = generateUniqueName("AUTO-COURSE-");
            String generateUniqueName = generateUniqueName("AUTO-QB-");
            item.setCourseName(uniqueCourseName);
            item.setQuestionBankName(generateUniqueName);

            try {
                CourseDto autoCourse = buildAutoCourse(uniqueCourseName);
                autoCourse = courseService.createCourseIfNotExists(autoCourse);

                ArchiveUploadResult uploadResult = processArchiveUpload(archive, autoCourse.getId(), generateUniqueName, studyYear, false);

                item.setStatus("PROCESSED");
                item.setFilesProcessed(uploadResult.filesProcessed());
                item.setMessage(uploadResult.toMessage());
                result.setProcessedArchives(result.getProcessedArchives() + 1);

                logger.atInfo().addArgument(item.getArchiveName()).addArgument(autoCourse.getId()).addArgument(generateUniqueName).addArgument(uploadResult.filesProcessed()).log(
                        "Processed folder item '{}' using courseId={}, questionBank='{}' -> {} files");

                if (archiveImportService != null) {
                    ArchiveImportDto archiveImport = archiveImportService.saveArchiveImport(item.getArchiveName(), fileSize, uploadResult.questionBankId());
                    result.getArchiveImports().add(archiveImport);
                }
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
                result.setFailedArchives(result.getFailedArchives() + 1);
                logger.atWarn().setCause(e).addArgument(item.getArchiveName()).log("Failed to process archive '{}' in folder upload");
            }

            result.getItems().add(item);
        }

        logger.atInfo().addArgument(result.getTotalArchives()).addArgument(result.getProcessedArchives()).addArgument(result.getSkippedArchives()).addArgument(result.getFailedArchives()).log(
                "Folder archive upload finished: total={}, processed={}, skipped={}, failed={}");

        return result;
    }

    /**
     * Process Excel file upload following upload-sd.md Section 2.2 specifications.
     * <p>
     * Steps:
     * 1. Save file to temporary location
     * 2. Create or retrieve author
     * 3. Retrieve course information
     * 4. Create questionBank with template type
     * 5. Parse Excel file and create questions
     * 6. Clean up temporary file
     * <p>
     * Note: duplicate detection is intentionally user-triggered via course recompute action.
     *
     * @param file             Excel file to process
     * @param username         Author name
     * @param courseId         Course ID
     * @param questionBankName QuestionBank short name (e.g., "Q1", "Q2")
     * @param templateType     Template type for parsing
     * @return Success message
     * @throws IllegalArgumentException if courseId is invalid
     */
    @Transactional
    public String processExcelUpload(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType) {

        validateExcelUploadInputs(file, username, courseId, questionBankName, templateType);

        logger.atInfo().addArgument(file.getOriginalFilename()).addArgument(username).log("Processing Excel upload: file='{}', author='{}'");

        // Step 1: Save file to temporary location
        final String filepath;
        try {
            filepath = fileService.uploadFile(file);
        } catch (IOException e) {
            logger.atError().setCause(e).addArgument(file.getOriginalFilename()).log("Failed to save Excel file '{}' before processing");
            throw new IllegalStateException("Could not save uploaded Excel file: " + e.getMessage(), e);
        }
        logger.atInfo().addArgument(filepath).log("File uploaded to: {}");

        try {
            // Step 2: Create or retrieve author
            AuthorDto authorDto = createOrRetrieveAuthor(username);

            // Step 3: Validate and retrieve course
            if (courseId == null || courseId == 0) {
                throw new IllegalArgumentException("Course ID is required");
            }
            CourseDto courseDto = courseService.findById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            // Step 4: Create questionBank with template type
            QuestionBank questionBank = createQuestionBank(questionBankName, courseDto.getCourse(), templateType);
            logger.atInfo().addArgument(questionBank.getId()).log("QuestionBank created with ID: {}");

            // Step 5: Parse Excel file and create questions
            Author authorEntity = authorService.findAuthorEntityById(authorDto.getId());
            if (authorEntity == null) {
                throw new IllegalStateException("Could not retrieve author entity for ID: " + authorDto.getId());
            }

            String parseResult = questionService.parseFileSheets(questionBank, authorEntity, filepath);
            logger.atInfo().addArgument(parseResult).log("Parse result: {}");

            if (parseResult == null || parseResult.isBlank()) {
                throw new IllegalStateException("Excel parsing produced an empty result");
            }
            if (isParseError(parseResult)) {
                throw new IllegalArgumentException("Failed to parse Excel file: " + parseResult);
            }

            // Step 6: Clean up temporary file
            fileService.removeFile(filepath);
            logger.atInfo().addArgument(filepath).log("Cleaned up file: {}");

            return "Successfully uploaded and processed file with " + authorEntity.getName();

        } catch (Exception e) {
            // Clean up on error
            try {
                fileService.removeFile(filepath);
            } catch (Exception cleanupEx) {
                logger.atWarn().setCause(cleanupEx).log("Failed to clean up file after error");
            }
            throw e;
        }
    }

    private void validateExcelUploadInputs(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(EXCEL_FILE_EXTENSION)) {
            throw new IllegalArgumentException("Only .xlsx files are supported for single Excel upload");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (questionBankName == null || questionBankName.isBlank()) {
            throw new IllegalArgumentException("QuestionBank name is required");
        }
        if (templateType == null) {
            throw new IllegalArgumentException("Template type is required");
        }
    }

    private boolean isParseError(String parseResult) {
        return parseResult.toLowerCase().contains("error");
    }

    private void validateXmlUploadInputs(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear) {
        if (xml == null || xml.isEmpty()) {
            throw new IllegalArgumentException("XML file is required");
        }
        String originalFilename = xml.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new IllegalArgumentException("Only .xml files are supported for XML upload");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (questionBankName == null || questionBankName.isBlank()) {
            throw new IllegalArgumentException("QuestionBank name is required");
        }
        if (studyYear == null) {
            throw new IllegalArgumentException("Study year is required");
        }
    }

    private Set<String> buildCourseQuestionCache(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return new HashSet<>();
        }
        Specification<Question> specification = QuestionSpecification.byCourse(courseName);
        List<Question> existingQuestions = questionRepository.findAll(specification);

        Set<String> cache = new HashSet<>();
        for (Question question : existingQuestions) {
            String key = buildQuestionKey(question.getTitle(), question.getText());
            if (key != null) {
                cache.add(key);
            }
        }
        return cache;
    }

    private String buildQuestionKey(String title, String text) {
        String normalizedTitle = normalizeForComparison(title);
        String normalizedText = normalizeForComparison(text);
        if (normalizedTitle == null || normalizedText == null) {
            return null;
        }
        return normalizedTitle + "||" + normalizedText;
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<String, Author> loadAuthorCacheByInitials() {
        Map<String, Author> authorsByInitials = new HashMap<>();
        // Use the cached getAllAuthorsBasic() to avoid expensive question-count queries
        for (com.unitbv.myquiz.api.dto.AuthorInfo authorInfo : authorService.getAllAuthorsBasic()) {
            if (authorInfo.getId() == null || authorInfo.getInitials() == null || authorInfo.getInitials().isBlank()) {
                continue;
            }
            Author author = authorService.findAuthorEntityById(authorInfo.getId());
            if (author != null) {
                authorsByInitials.putIfAbsent(authorInfo.getInitials().trim().toUpperCase(Locale.ROOT), author);
            }
        }
        return authorsByInitials;
    }

    private Author resolveAuthor(String initials, Map<String, Author> authorsByInitials) {
        if (initials != null && !initials.isBlank()) {
            String normalizedInitials = initials.trim().toUpperCase(Locale.ROOT);
            Author existing = authorsByInitials.get(normalizedInitials);
            if (existing != null) {
                logger.atInfo().addArgument(normalizedInitials).log("Resolved author by initials: {}");
                return existing;
            }
            logger.atWarn().addArgument(normalizedInitials).log("Author initials '{}' not found in cache – assigning to dummy author");
        } else {
            logger.atWarn().log("Blank/null author initials encountered – assigning to dummy author");
        }
        // Initials not found (or blank) — use a single shared dummy author for all unresolved records
        return authorsByInitials.computeIfAbsent("__DUMMY__", key -> authorService.findOrCreateDummyAuthor());
    }

    private List<QuestionDto> parseXmlQuestions(MultipartFile xml) {
        try (InputStream inputStream = xml.getInputStream()) {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList questionNodes = document.getElementsByTagName("question");
            java.util.ArrayList<QuestionDto> parsed = new java.util.ArrayList<>();
            for (int i = 0; i < questionNodes.getLength(); i++) {
                Node node = questionNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element questionElement = (Element) node;
                String xmlType = questionElement.getAttribute("type");
                if (!"multichoice".equalsIgnoreCase(xmlType) && !"truefalse".equalsIgnoreCase(xmlType)) {
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
                dto.setAuthorName(parsedName.initials());
                dto.setText(text);

                if ("truefalse".equalsIgnoreCase(xmlType)) {
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

        // Fallback keeps import resilient for exports using non-standard answer labels.
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

    /**
     * Process archive (ZIP) upload following upload-sd.md Section 2.4 specifications.
     * OPTIMIZED for performance with batch processing and reduced transaction scope.
     * <p>
     * Steps:
     * 1. Create temporary directory
     * 2. Save archive file
     * 3. Extract ZIP contents (outside transaction)
     * 4. Retrieve course information (outside transaction)
     * 5. Create questionBank (in transaction)
     * 6. Parse all Excel files from extracted folder (in transaction with batch saves)
     * 7. Clean up temporary directory and archive
     * <p>
     * Note: duplicate detection is intentionally user-triggered via course recompute action.
     *
     * @param archive          ZIP archive file
     * @param courseId         Course ID
     * @param questionBankName QuestionBank name
     * @param studyYear        QuestionBank study year
     * @return Result with number of files processed
     * @throws IOException              if file operations fail
     * @throws IllegalArgumentException if courseId is invalid
     */
    public ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear) throws IOException {
        return processArchiveUpload(archive, courseId, questionBankName, studyYear, true);
    }

    private ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear,
                                                     boolean persistArchiveImport) throws IOException {

        long startTime = System.currentTimeMillis();
        logger.atInfo().addArgument(archive.getOriginalFilename()).addArgument(questionBankName).log("Processing archive upload: file='{}', questionBank='{}'");

        Path tempDir = null;
        String archivePath = null;

        try {
            // Step 1: Create temporary directory (fast, outside transaction)
            tempDir = Files.createTempDirectory("uploaded-archive-");
            if (!Files.isWritable(tempDir)) {
                throw new IOException("Temp directory is not writable: " + tempDir);
            }
            logger.atInfo().addArgument(tempDir).log("Created temp directory: {}");

            // Step 2: Save archive file (I/O intensive, outside transaction)
            archivePath = fileService.uploadFile(archive);
            Path archiveFilePath = Path.of(archivePath);
            logger.atInfo().addArgument(archivePath).log("Archive uploaded to: {}");

            if (!FileValidator.exists(archiveFilePath)) {
                throw new IOException("Archive file was not saved to disk: " + archivePath);
            }

            // Step 3: Extract ZIP contents (I/O intensive, outside transaction)
            long extractStart = System.currentTimeMillis();
            extractArchiveContents(archiveFilePath, tempDir);
            logger.atInfo().addArgument(System.currentTimeMillis() - extractStart).log("Archive extracted in {}ms");

            // Step 4: Validate course (outside transaction)
            CourseDto courseDto = findCourseById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            // Step 5-6: Process files in optimized transaction
            ArchiveUploadResult uploadResult = processFilesFromArchiveOptimized(courseDto, questionBankName, studyYear, tempDir);

            if (persistArchiveImport && archiveImportService != null) {
                archiveImportService.saveArchiveImport(
                        safeArchiveName(archive.getOriginalFilename()),
                        archive.getSize(),
                        uploadResult.questionBankId()
                );
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.atInfo().addArgument(uploadResult.filesProcessed()).addArgument(totalTime).log("Archive upload completed: {} files processed in {}ms");

            return uploadResult;

        } catch (Exception e) {
            logger.atError().setCause(e).log("Archive upload failed, cleaning up...");
            cleanup(tempDir, archivePath);
            throw e;
        } finally {
            // Always cleanup
            cleanup(tempDir, archivePath);
        }
    }

    /**
     * Optimized version of processFilesFromArchive with better transaction management.
     * Uses a single transaction for questionBank creation, parsing, and validation.
     *
     * @param courseDto        Course information (pre-fetched)
     * @param questionBankName QuestionBank name
     * @param studyYear        QuestionBank study year
     * @param tempDir          Temporary directory with extracted files
     * @return Number of files processed
     */
    @Transactional
    protected ArchiveUploadResult processFilesFromArchiveOptimized(CourseDto courseDto, String questionBankName, StudyYear studyYear, Path tempDir) {

        long transactionStart = System.currentTimeMillis();

        // Create questionBank
        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(questionBankName);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(courseDto.getCourse()));
        questionBank.setStudyYear(studyYear);
        questionBank = questionService.saveQuestionBank(questionBank);
        logger.atInfo().addArgument(questionBank.getId()).log("QuestionBank created with ID: {}");

        // Parse all Excel files (uses batch saves internally)
        long parseStart = System.currentTimeMillis();
        int filesProcessed = questionService.parseExcelFilesFromFolder(questionBank, tempDir.toFile(), 0);
        logger.atInfo().addArgument(filesProcessed).addArgument(System.currentTimeMillis() - parseStart).log("Parsed {} files in {}ms");

        // Duplicate recompute is user-triggered from the course action.

        logger.atInfo().addArgument(System.currentTimeMillis() - transactionStart).log("Transaction completed in {}ms");

        return new ArchiveUploadResult(filesProcessed, questionBankName, questionBank.getId());
    }


    /**
     * Create or retrieve author by name.
     *
     * @param username Author name
     * @return AuthorDto (existing or newly created)
     */
    private AuthorDto createOrRetrieveAuthor(String username) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(username);
        authorDto.setInitials(authorService.extractInitials(username));

        if (authorService.authorNameExists(authorDto.getName())) {
            logger.atInfo().addArgument(authorDto.getName()).log("Author '{}' already exists");
            return authorService.getAuthorByName(authorDto.getName());
        } else {
            authorDto = authorService.saveAuthorDto(authorDto);
            logger.atInfo().addArgument(authorDto.getName()).log("Created new author: '{}'");
            return authorDto;
        }
    }

    /**
     * Create questionBank with specified parameters.
     *
     * @param name         QuestionBank name
     * @param course       Course name
     * @param templateType Template type
     * @return Created QuestionBank entity
     */
    private QuestionBank createQuestionBank(String name, String course, TemplateType templateType) {
        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(name);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(course));
        questionBank.setStudyYear(null);
        return questionService.saveQuestionBank(questionBank);
    }

    private void extractArchiveContents(Path archiveFilePath, Path tempDir) throws IOException {
        try {
            fileService.unzipAndRenameExcelFiles(archiveFilePath, tempDir);
        } catch (ZipException e) {
            throw new IllegalArgumentException("Invalid or unsupported ZIP archive: " + e.getMessage(), e);
        }
    }

    /**
     * Find course by ID from all courses.
     *
     * @param courseId Course ID
     * @return CourseDto or null if not found
     */
    private CourseDto findCourseById(Long courseId) {
        return courseService.getAllCourses().stream().filter(c -> c.getId().equals(courseId)).findFirst().orElse(null);
    }

    /**
     * Clean up temporary files and directories.
     *
     * @param tempDir     Temporary directory to delete
     * @param archivePath Archive file path to delete
     */
    private void cleanup(Path tempDir, String archivePath) {
        if (FileValidator.exists(tempDir)) {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
                logger.atInfo().addArgument(tempDir).log("Deleted temp directory: {}");
            } catch (IOException e) {
                logger.atWarn().setCause(e).addArgument(tempDir).log("Failed to delete temp directory: {}");
            }
        }

        if (archivePath != null) {
            try {
                fileService.removeFile(archivePath);
                logger.atInfo().addArgument(archivePath).log("Deleted archive file: {}");
            } catch (Exception e) {
                logger.atWarn().setCause(e).addArgument(archivePath).log("Failed to delete archive file: {}");
            }
        }
    }

    private CourseDto buildAutoCourse(String courseName) {
        CourseDto courseDto = new CourseDto();
        courseDto.setCourse(courseName);
        courseDto.setDescription("Auto-generated for archive folder processing");
        courseDto.setSemester("1");
        courseDto.setUniversityYear("1");
        return courseDto;
    }

    private String generateUniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String safeArchiveName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unknown.zip";
        }
        int slash = Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\'));
        return slash >= 0 ? originalName.substring(slash + 1) : originalName;
    }

    private boolean isZipArchive(MultipartFile archive) {
        String fileName = archive.getOriginalFilename();
        return fileName != null && fileName.toLowerCase().endsWith(".zip");
    }

    private record ParsedExportedName(String initials, String chapter, String title) {
    }

}
