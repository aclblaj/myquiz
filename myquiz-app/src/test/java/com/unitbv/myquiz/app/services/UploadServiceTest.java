package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private QuestionService questionService;
    @Mock
    private AuthorService authorService;
    @Mock
    private FileService fileService;
    @Mock
    private CourseService courseService;
    @Mock
    private ArchiveImportService archiveImportService;
    @Mock
    private QuestionBankAuthorRepository questionBankAuthorRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private MultipartFile multipartFile;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(
                questionService,
                authorService,
                fileService,
                courseService,
                archiveImportService,
                questionBankAuthorRepository,
                questionRepository
        );
    }

    @Test
    void processExcelUpload_whenParserReturnsError_throwsAndCleansUp() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("sample.xlsx");
        when(fileService.uploadFile(multipartFile)).thenReturn("C:/tmp/sample.xlsx");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(11L);
        authorDto.setName(ServiceTestData.AUTHOR_NAME);
        when(authorService.extractInitials(ServiceTestData.AUTHOR_NAME)).thenReturn(ServiceTestData.AUTHOR_INITIALS);
        when(authorService.authorNameExists(ServiceTestData.AUTHOR_NAME)).thenReturn(true);
        when(authorService.getAuthorByName(ServiceTestData.AUTHOR_NAME)).thenReturn(authorDto);

        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.COURSE).build();
        courseDto.setId(20L);
        when(courseService.findById(20L)).thenReturn(courseDto);

        Course courseEntity = new Course();
        courseEntity.setId(20L);
        courseEntity.setCourse(ServiceTestData.COURSE);
        when(courseService.getOrCreateCourseEntity(ServiceTestData.COURSE)).thenReturn(courseEntity);

        QuestionBank savedQuestionBank = new QuestionBank();
        savedQuestionBank.setId(30L);
        savedQuestionBank.setName(ServiceTestData.QUESTION_BANK_NAME);
        when(questionService.saveQuestionBank(any(QuestionBank.class))).thenReturn(savedQuestionBank);

        Author authorEntity = new Author();
        authorEntity.setId(11L);
        authorEntity.setName(ServiceTestData.AUTHOR_NAME);
        when(authorService.findAuthorEntityById(11L)).thenReturn(authorEntity);

        when(questionService.parseFileSheets(savedQuestionBank, authorEntity, "C:/tmp/sample.xlsx")).thenReturn("Error: Only .xlsx files are supported");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uploadService.processExcelUpload(
                multipartFile,
                ServiceTestData.AUTHOR_NAME,
                20L,
                ServiceTestData.QUESTION_BANK_NAME,
                TemplateType.Template2024
        ));

        assertEquals("Failed to parse Excel file: Error: Only .xlsx files are supported", ex.getMessage());
        verify(fileService).removeFile("C:/tmp/sample.xlsx");
    }

    @Test
    void processExcelUpload_whenFileIsNotXlsx_rejectsBeforeSaving() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("sample.xls");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uploadService.processExcelUpload(
                multipartFile,
                ServiceTestData.AUTHOR_NAME,
                20L,
                ServiceTestData.QUESTION_BANK_NAME,
                TemplateType.Template2024
        ));

        assertEquals("Only .xlsx files are supported for single Excel upload", ex.getMessage());
        verify(fileService, never()).uploadFile(any());
    }

    @Test
    void processXmlUpload_skipsExistingDuplicateByTitleAndText() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("import.xml");
        when(multipartFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream((
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<quiz>" +
                        "<question type=\"multichoice\">" +
                        "<name><text>CH-AB-SQL Basics</text></name>" +
                        "<questiontext format=\"html\"><text><![CDATA[What does SELECT do?]]></text></questiontext>" +
                        "<answer fraction=\"100\" format=\"html\"><text><![CDATA[Reads data]]></text></answer>" +
                        "<answer fraction=\"-100\" format=\"html\"><text><![CDATA[Deletes data]]></text></answer>" +
                        "<answer fraction=\"-100\" format=\"html\"><text><![CDATA[Drops table]]></text></answer>" +
                        "<answer fraction=\"-100\" format=\"html\"><text><![CDATA[Creates index]]></text></answer>" +
                        "</question>" +
                        "</quiz>"
        ).getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.COURSE).build();
        courseDto.setId(10L);
        when(courseService.getAllCourses()).thenReturn(java.util.List.of(courseDto));

        Course courseEntity = new Course();
        courseEntity.setId(10L);
        courseEntity.setCourse(ServiceTestData.COURSE);
        when(courseService.getOrCreateCourseEntity(ServiceTestData.COURSE)).thenReturn(courseEntity);

        QuestionBank qb = new QuestionBank();
        qb.setId(55L);
        when(questionService.saveQuestionBank(any(QuestionBank.class))).thenReturn(qb);

        Question existing = new Question();
        existing.setTitle("SQL Basics");
        existing.setText("What does SELECT do?");
        when(questionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(java.util.List.of(existing));
        when(authorService.getAllAuthors()).thenReturn(java.util.List.of());

        String result = uploadService.processXmlUpload(multipartFile, 10L, ServiceTestData.QUESTION_BANK_NAME, StudyYear.Y2024_2025);

        assertTrue(result.contains("Imported 0 question(s)"));
        assertTrue(result.contains("skipped 1 duplicate(s)"));
        verify(questionService, times(0)).saveQuestion(any(Question.class));
    }
}



