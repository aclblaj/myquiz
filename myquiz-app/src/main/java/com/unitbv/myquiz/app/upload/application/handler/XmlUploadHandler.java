package com.unitbv.myquiz.app.upload.application.handler;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.QuestionService;
import com.unitbv.myquiz.app.upload.application.support.UploadCourseLookupSupport;
import com.unitbv.myquiz.app.upload.application.support.UploadNamingSupport;
import com.unitbv.myquiz.app.upload.domain.parsing.XmlQuestionParser;
import com.unitbv.myquiz.app.upload.domain.policy.QuestionDeduplicationPolicy;
import com.unitbv.myquiz.app.upload.domain.policy.UploadAuthorResolutionPolicy;
import com.unitbv.myquiz.app.upload.domain.validation.UploadInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class XmlUploadHandler {
    private static final Logger logger = LoggerFactory.getLogger(XmlUploadHandler.class);

    private final QuestionService questionService;
    private final CourseService courseService;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final UploadInputValidator uploadInputValidator;
    private final XmlQuestionParser xmlQuestionParser;
    private final QuestionDeduplicationPolicy questionDeduplicationPolicy;
    private final UploadAuthorResolutionPolicy uploadAuthorResolutionPolicy;
    private final UploadCourseLookupSupport uploadCourseLookupSupport;
    private final UploadNamingSupport uploadNamingSupport;

    public XmlUploadHandler(QuestionService questionService,
                            CourseService courseService,
                            QuestionBankAuthorRepository questionBankAuthorRepository,
                            UploadInputValidator uploadInputValidator,
                            XmlQuestionParser xmlQuestionParser,
                            QuestionDeduplicationPolicy questionDeduplicationPolicy,
                            UploadAuthorResolutionPolicy uploadAuthorResolutionPolicy,
                            UploadCourseLookupSupport uploadCourseLookupSupport,
                            UploadNamingSupport uploadNamingSupport) {
        this.questionService = questionService;
        this.courseService = courseService;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.uploadInputValidator = uploadInputValidator;
        this.xmlQuestionParser = xmlQuestionParser;
        this.questionDeduplicationPolicy = questionDeduplicationPolicy;
        this.uploadAuthorResolutionPolicy = uploadAuthorResolutionPolicy;
        this.uploadCourseLookupSupport = uploadCourseLookupSupport;
        this.uploadNamingSupport = uploadNamingSupport;
    }

    @Transactional
    public String processXmlUpload(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear) {
        uploadInputValidator.validateXmlUploadInputs(xml, courseId, questionBankName, studyYear);

        CourseDto courseDto = uploadCourseLookupSupport.findCourseById(courseId);
        if (courseDto == null) {
            throw new IllegalArgumentException("Course not found for ID: " + courseId);
        }

        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(questionBankName);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(courseDto.getCourse()));
        questionBank.setStudyYear(studyYear);
        final QuestionBank persistedQuestionBank = questionService.saveQuestionBank(questionBank);

        Set<String> courseQuestionCache = questionDeduplicationPolicy.buildCourseQuestionCache(courseDto.getCourse());
        Map<String, Author> authorsByInitials = uploadAuthorResolutionPolicy.loadAuthorCacheByInitials();
        Map<Long, QuestionBankAuthor> relationByAuthorId = new HashMap<>();

        List<QuestionDto> parsedQuestions = xmlQuestionParser.parseXmlQuestions(xml);
        int imported = 0;
        int skippedDuplicates = 0;
        int invalidQuestions = 0;
        int crtNo = 1;

        for (QuestionDto parsed : parsedQuestions) {
            String key = questionDeduplicationPolicy.buildQuestionKey(parsed.getTitle(), parsed.getText());
            if (key == null) {
                invalidQuestions++;
                continue;
            }
            if (courseQuestionCache.contains(key)) {
                skippedDuplicates++;
                continue;
            }

            AuthorInfo parsedAuthor = parsed.getAuthor();
            String parsedInitials = parsedAuthor != null ? parsedAuthor.getInitials() : null;
            Author author = uploadAuthorResolutionPolicy.resolveAuthor(parsedInitials, authorsByInitials);
            QuestionBankAuthor relation = relationByAuthorId.computeIfAbsent(author.getId(), authorId -> {
                QuestionBankAuthor qba = new QuestionBankAuthor();
                qba.setAuthor(author);
                qba.setQuestionBank(persistedQuestionBank);
                qba.setSource(uploadNamingSupport.safeArchiveName(xml.getOriginalFilename()));
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


}
