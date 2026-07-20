package com.unitbv.myquiz.app.repositories;

import com.unitbv.myquiz.app.entities.DuplicateRecomputeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DuplicateRecomputeHistory entities.
 */
@Repository
public interface DuplicateRecomputeHistoryRepository extends JpaRepository<DuplicateRecomputeHistory, Long> {

    /**
     * Returns all history entries ordered by savedAt descending (most recent first).
     */
    List<DuplicateRecomputeHistory> findAllByOrderBySavedAtDesc();

    /**
     * Returns history entries for a specific course ordered by savedAt descending.
     */
    List<DuplicateRecomputeHistory> findByCourseIdOrderBySavedAtDesc(Long courseId);
}

