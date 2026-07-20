package com.unitbv.myquiz.app.upload.application.handler;

import com.unitbv.myquiz.api.dto.ArchiveFolderItemDto;
import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveImportDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.services.ArchiveImportService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.upload.application.support.ArchiveFolderResultSupport;
import com.unitbv.myquiz.app.upload.application.support.UploadNamingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class ArchiveFolderUploadHandler {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveFolderUploadHandler.class);

    private final ArchiveUploadHandler archiveUploadHandler;
    private final ArchiveImportService archiveImportService;
    private final CourseService courseService;
    private final ArchiveFolderResultSupport archiveFolderResultSupport;
    private final UploadNamingSupport uploadNamingSupport;

    public ArchiveFolderUploadHandler(ArchiveUploadHandler archiveUploadHandler,
                                      ArchiveImportService archiveImportService,
                                      CourseService courseService,
                                      ArchiveFolderResultSupport archiveFolderResultSupport,
                                      UploadNamingSupport uploadNamingSupport) {
        this.archiveUploadHandler = archiveUploadHandler;
        this.archiveImportService = archiveImportService;
        this.courseService = courseService;
        this.archiveFolderResultSupport = archiveFolderResultSupport;
        this.uploadNamingSupport = uploadNamingSupport;
    }

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
            ArchiveFolderItemDto item = archiveFolderResultSupport.createItem(
                    index,
                    inputArchives.size(),
                    uploadNamingSupport.safeArchiveName(archive.getOriginalFilename())
            );

            logger.atInfo().addArgument(index).addArgument(inputArchives.size()).addArgument(item.getArchiveName()).log("Folder archive item {}/{}: {}");

            if (!uploadNamingSupport.isZipArchive(archive)) {
                archiveFolderResultSupport.markSkipped(result, item, ArchiveFolderResultSupport.MSG_SKIPPED_NON_ARCHIVE);
                logger.atInfo().addArgument(item.getArchiveName()).log("Skipping folder item '{}' because it is not a ZIP archive");
                result.getItems().add(item);
                continue;
            }

            long fileSize = archive.getSize();
            if (archiveImportService != null && archiveImportService.existsBySize(fileSize)) {
                archiveFolderResultSupport.markSkipped(result, item, ArchiveFolderResultSupport.MSG_SKIPPED_DUPLICATE_SIZE);
                logger.atInfo().addArgument(item.getArchiveName()).addArgument(fileSize).log("Skipping folder item '{}' because an archive with size {} was already processed");
                result.getItems().add(item);
                continue;
            }

            String uniqueCourseName = uploadNamingSupport.generateUniqueName("AUTO-COURSE-");
            String uniqueQuestionBankName = uploadNamingSupport.generateUniqueName("AUTO-QB-");
            item.setCourseName(uniqueCourseName);
            item.setQuestionBankName(uniqueQuestionBankName);

            try {
                CourseDto autoCourse = uploadNamingSupport.buildAutoCourse(uniqueCourseName);
                autoCourse = courseService.createCourseIfNotExists(autoCourse);

                ArchiveUploadResult uploadResult = archiveUploadHandler.processArchiveUpload(archive, autoCourse.getId(), uniqueQuestionBankName, studyYear, false);

                archiveFolderResultSupport.markProcessed(result, item, uploadResult);

                logger.atInfo().addArgument(item.getArchiveName()).addArgument(autoCourse.getId()).addArgument(uniqueQuestionBankName).addArgument(uploadResult.filesProcessed()).log(
                        "Processed folder item '{}' using courseId={}, questionBank='{}' -> {} files");

                if (archiveImportService != null) {
                    ArchiveImportDto archiveImport = archiveImportService.saveArchiveImport(item.getArchiveName(), fileSize, uploadResult.questionBankId());
                    result.getArchiveImports().add(archiveImport);
                }
            } catch (Exception e) {
                archiveFolderResultSupport.markFailed(result, item, e);
                logger.atWarn().setCause(e).addArgument(item.getArchiveName()).log("Failed to process archive '{}' in folder upload");
            }

            result.getItems().add(item);
        }

        logger.atInfo().addArgument(result.getTotalArchives()).addArgument(result.getProcessedArchives()).addArgument(result.getSkippedArchives()).addArgument(result.getFailedArchives()).log(
                "Folder archive upload finished: total={}, processed={}, skipped={}, failed={}");

        return result;
    }

}

