package com.unitbv.myquiz.app.upload.application;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadApplicationService {
    String processXmlUpload(MultipartFile xml, Long courseId, String questionBankName, StudyYear studyYear);

    ArchiveFolderUploadResultDto processArchiveFolderUpload(MultipartFile[] archives, StudyYear studyYear);

    String processExcelUpload(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType);

    ArchiveUploadResult processArchiveUpload(MultipartFile archive, Long courseId, String questionBankName, StudyYear studyYear) throws IOException;
}

