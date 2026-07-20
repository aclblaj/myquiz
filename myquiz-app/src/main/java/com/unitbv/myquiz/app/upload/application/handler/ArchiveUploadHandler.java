package com.unitbv.myquiz.app.upload.application.handler;

import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.services.ArchiveImportService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.FileService;
import com.unitbv.myquiz.app.services.QuestionService;
import com.unitbv.myquiz.app.upload.application.support.ArchiveProcessingSupport;
import com.unitbv.myquiz.app.upload.application.support.UploadCourseLookupSupport;
import com.unitbv.myquiz.app.upload.application.support.UploadNamingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class ArchiveUploadHandler {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveUploadHandler.class);

    private final QuestionService questionService;
    private final FileService fileService;
    private final CourseService courseService;
    private final ArchiveImportService archiveImportService;
    private final UploadCourseLookupSupport uploadCourseLookupSupport;
    private final ArchiveProcessingSupport archiveProcessingSupport;
    private final UploadNamingSupport uploadNamingSupport;

    public ArchiveUploadHandler(QuestionService questionService,
                                FileService fileService,
                                CourseService courseService,
                                ArchiveImportService archiveImportService,
                                UploadCourseLookupSupport uploadCourseLookupSupport,
                                ArchiveProcessingSupport archiveProcessingSupport,
                                UploadNamingSupport uploadNamingSupport) {
        this.questionService = questionService;
        this.fileService = fileService;
        this.courseService = courseService;
        this.archiveImportService = archiveImportService;
        this.uploadCourseLookupSupport = uploadCourseLookupSupport;
        this.archiveProcessingSupport = archiveProcessingSupport;
        this.uploadNamingSupport = uploadNamingSupport;
    }

    public ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear) throws IOException {
        return processArchiveUpload(archive, courseId, questionBankName, studyYear, true);
    }

    public ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear,
                                                    boolean persistArchiveImport) throws IOException {

        long startTime = System.currentTimeMillis();
        logger.atInfo().addArgument(archive.getOriginalFilename()).addArgument(questionBankName).log("Processing archive upload: file='{}', questionBank='{}'");

        Path tempDir = null;
        String archivePath = null;

        try {
            tempDir = archiveProcessingSupport.createWritableTempDirectory("uploaded-archive-");
            logger.atInfo().addArgument(tempDir).log("Created temp directory: {}");

            archivePath = fileService.uploadFile(archive);
            Path archiveFilePath = Path.of(archivePath);
            logger.atInfo().addArgument(archivePath).log("Archive uploaded to: {}");

            archiveProcessingSupport.ensureUploadedArchiveExists(archiveFilePath);

            long extractStart = System.currentTimeMillis();
            archiveProcessingSupport.extractArchiveContents(archiveFilePath, tempDir);
            logger.atInfo().addArgument(System.currentTimeMillis() - extractStart).log("Archive extracted in {}ms");

            CourseDto courseDto = uploadCourseLookupSupport.findCourseById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            ArchiveUploadResult uploadResult = processFilesFromArchiveOptimized(courseDto, questionBankName, studyYear, tempDir);

            if (persistArchiveImport && archiveImportService != null) {
                archiveImportService.saveArchiveImport(
                        uploadNamingSupport.safeArchiveName(archive.getOriginalFilename()),
                        archive.getSize(),
                        uploadResult.questionBankId()
                );
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.atInfo().addArgument(uploadResult.filesProcessed()).addArgument(totalTime).log("Archive upload completed: {} files processed in {}ms");

            return uploadResult;

        } catch (Exception e) {
            logger.atError().setCause(e).log("Archive upload failed, cleaning up...");
            throw e;
        } finally {
            archiveProcessingSupport.cleanup(tempDir, archivePath);
        }
    }

    @Transactional
    protected ArchiveUploadResult processFilesFromArchiveOptimized(CourseDto courseDto, String questionBankName, StudyYear studyYear, Path tempDir) {

        long transactionStart = System.currentTimeMillis();

        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(questionBankName);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(courseDto.getCourse()));
        questionBank.setStudyYear(studyYear);
        questionBank = questionService.saveQuestionBank(questionBank);
        logger.atInfo().addArgument(questionBank.getId()).log("QuestionBank created with ID: {}");

        long parseStart = System.currentTimeMillis();
        int filesProcessed = questionService.parseExcelFilesFromFolder(questionBank, tempDir.toFile(), 0);
        logger.atInfo().addArgument(filesProcessed).addArgument(System.currentTimeMillis() - parseStart).log("Parsed {} files in {}ms");

        logger.atInfo().addArgument(System.currentTimeMillis() - transactionStart).log("Transaction completed in {}ms");

        return new ArchiveUploadResult(filesProcessed, questionBankName, questionBank.getId());
    }


}

