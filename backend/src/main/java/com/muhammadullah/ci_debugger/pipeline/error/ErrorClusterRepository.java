package com.muhammadullah.ci_debugger.pipeline.error;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ErrorClusterRepository extends JpaRepository<ErrorCluster, UUID> {

    Optional<ErrorCluster> findByFingerprint(String fingerprint);

    /**
     * Returns clusters ordered by occurrence count descending. 
     */
    List<ErrorCluster> findAllByOrderByOccurrenceCountDesc(Pageable pageable);

    /**
     * Returns all clusters that have at least one occurrence in the given run,
     * ordered by occurrence count descending.
     */
    @Query("""
            SELECT DISTINCT c FROM ErrorCluster c
            JOIN ErrorOccurrence o ON o.errorCluster = c
            WHERE o.pipelineRun.id = :runId
            ORDER BY c.occurrenceCount DESC
            """)
    List<ErrorCluster> findByRunId(@Param("runId") UUID runId);
}