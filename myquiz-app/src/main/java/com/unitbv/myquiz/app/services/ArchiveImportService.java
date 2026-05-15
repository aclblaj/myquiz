package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.ArchiveImportDto;
import com.unitbv.myquiz.app.entities.ArchiveImport;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.repositories.ArchiveImportRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchiveImportService {

    private final ArchiveImportRepository archiveImportRepository;
    private final QuestionBankRepository questionBankRepository;

    public ArchiveImportService(ArchiveImportRepository archiveImportRepository,
                                QuestionBankRepository questionBankRepository) {
        this.archiveImportRepository = archiveImportRepository;
        this.questionBankRepository = questionBankRepository;
    }

    @Transactional(readOnly = true)
    public boolean existsBySize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            return false;
        }
        return archiveImportRepository.existsByFileSize(fileSize);
    }

    @Transactional
    public ArchiveImportDto saveArchiveImport(String fileName, Long fileSize, Long questionBankId) {
        QuestionBank foundQuestionBank = questionBankId != null ? questionBankRepository.findById(questionBankId).orElse(null) : null;
        QuestionBank questionBank = toQuestionBank(foundQuestionBank);
        ArchiveImport entity = new ArchiveImport(fileName, fileSize, questionBank);
        entity = archiveImportRepository.save(entity);
        return toDto(entity);
    }

    private QuestionBank toQuestionBank(QuestionBank inpQuestionBank) {
        if (inpQuestionBank == null) {
            return null;
        }
        QuestionBank questionBank = new QuestionBank();
        questionBank.setId(inpQuestionBank.getId());
        questionBank.setName(inpQuestionBank.getName());
        questionBank.setCourse(inpQuestionBank.getCourse());
        questionBank.setStudyYear(inpQuestionBank.getStudyYear());
        return questionBank;
    }

    private ArchiveImportDto toDto(ArchiveImport entity) {
        return new ArchiveImportDto(
            entity.getId(),
            entity.getFileName(),
            entity.getFileSize(),
            entity.getProcessedAt()
        );
    }
}


