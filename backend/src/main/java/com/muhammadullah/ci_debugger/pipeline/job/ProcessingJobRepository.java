package com.muhammadullah.ci_debugger.pipeline.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    @Query("""
            SELECT j FROM ProcessingJob j
            WHERE j.status IN ('PENDING')
            AND j.scheduledAt <= :now
            AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
            """)
    List<ProcessingJob> findEligibleJobs(@Param("now") Instant now);

}