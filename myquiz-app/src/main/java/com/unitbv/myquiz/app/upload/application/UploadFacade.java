package com.unitbv.myquiz.app.upload.application;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.upload.application.handler.ArchiveFolderUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.ArchiveUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.ExcelUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.XmlUploadHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UploadFacade implements UploadApplicationService {
    private final XmlUploadHandler xmlUploadHandler;
    private final ExcelUploadHandler excelUploadHandler;
    private final ArchiveUploadHandler archiveUploadHandler;
    private final ArchiveFolderUploadHandler archiveFolderUploadHandler;

    public UploadFacade(XmlUploadHandler xmlUploadHandler,
                        ExcelUploadHandler excelUploadHandler,
                        ArchiveUploadHandler archiveUploadHandler,
                        ArchiveFolderUploadHandler archiveFolderUploadHandler) {
        this.xmlUploadHandler = xmlUploadHandler;
        this.excelUploadHandler = excelUploadHandler;
        this.archiveUploadHandler = archiveUploadHandler;
        this.archiveFolderUploadHandler = archiveFolderUploadHandler;
    }

    @Override
    public String processXmlUpload(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear) {
        return xmlUploadHandler.processXmlUpload(xml, courseId, questionBankName, studyYear);
    }

    @Override
    public ArchiveFolderUploadResultDto processArchiveFolderUpload(MultipartFile[] archives, StudyYear studyYear) {
        return archiveFolderUploadHandler.processArchiveFolderUpload(archives, studyYear);
    }

    @Override
    public String processExcelUpload(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType) {
        return excelUploadHandler.processExcelUpload(file, username, courseId, questionBankName, templateType);
    }

    @Override
    public ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear) throws IOException {
        return archiveUploadHandler.processArchiveUpload(archive, courseId, questionBankName, studyYear);
    }
}

