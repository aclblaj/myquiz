package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.ArchiveImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchiveImportRepository extends JpaRepository<ArchiveImport, Long> {

    boolean existsByFileSize(Long fileSize);
}


