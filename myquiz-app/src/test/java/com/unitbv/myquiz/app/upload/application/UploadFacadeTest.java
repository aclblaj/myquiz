package com.unitbv.myquiz.app.upload.application;

import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.upload.application.handler.ArchiveFolderUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.ArchiveUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.ExcelUploadHandler;
import com.unitbv.myquiz.app.upload.application.handler.XmlUploadHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadFacadeTest {

    @Mock
    private XmlUploadHandler xmlUploadHandler;
    @Mock
    private ExcelUploadHandler excelUploadHandler;
    @Mock
    private ArchiveUploadHandler archiveUploadHandler;
    @Mock
    private ArchiveFolderUploadHandler archiveFolderUploadHandler;

    private UploadFacade uploadFacade;

    @BeforeEach
    void setUp() {
        uploadFacade = new UploadFacade(
                xmlUploadHandler,
                excelUploadHandler,
                archiveUploadHandler,
                archiveFolderUploadHandler
        );
    }

    @Test
    void processExcelUpload_delegatesToExcelHandler() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(excelUploadHandler.processExcelUpload(file, "user", 20L, "QB", TemplateType.Template2024)).thenReturn("ok");

        String result = uploadFacade.processExcelUpload(file, "user", 20L, "QB", TemplateType.Template2024);

        assertEquals("ok", result);
        verify(excelUploadHandler).processExcelUpload(file, "user", 20L, "QB", TemplateType.Template2024);
    }

    @Test
    void processXmlUpload_delegatesToXmlHandler() {
        MultipartFile xml = org.mockito.Mockito.mock(MultipartFile.class);
        when(xmlUploadHandler.processXmlUpload(xml, 10L, "QB", StudyYear.Y2024_2025)).thenReturn("xml-ok");

        String result = uploadFacade.processXmlUpload(xml, 10L, "QB", StudyYear.Y2024_2025);

        assertEquals("xml-ok", result);
        verify(xmlUploadHandler).processXmlUpload(xml, 10L, "QB", StudyYear.Y2024_2025);
    }

    @Test
    void processArchiveUpload_delegatesToArchiveHandler() throws Exception {
        MultipartFile archive = org.mockito.Mockito.mock(MultipartFile.class);
        ArchiveUploadResult expected = new ArchiveUploadResult(2, "QB", 10L);
        when(archiveUploadHandler.processArchiveUpload(archive, 20L, "QB", StudyYear.Y2026_2027)).thenReturn(expected);

        ArchiveUploadResult result = uploadFacade.processArchiveUpload(archive, 20L, "QB", StudyYear.Y2026_2027);

        assertEquals(expected, result);
        verify(archiveUploadHandler).processArchiveUpload(archive, 20L, "QB", StudyYear.Y2026_2027);
    }

    @Test
    void processArchiveFolderUpload_delegatesToArchiveFolderHandler() {
        MultipartFile[] archives = new MultipartFile[] {org.mockito.Mockito.mock(MultipartFile.class)};
        ArchiveFolderUploadResultDto expected = new ArchiveFolderUploadResultDto();
        expected.setTotalArchives(1);
        when(archiveFolderUploadHandler.processArchiveFolderUpload(archives, StudyYear.Y2025_2026)).thenReturn(expected);

        ArchiveFolderUploadResultDto result = uploadFacade.processArchiveFolderUpload(archives, StudyYear.Y2025_2026);

        assertEquals(1, result.getTotalArchives());
        verify(archiveFolderUploadHandler).processArchiveFolderUpload(archives, StudyYear.Y2025_2026);
    }
}




