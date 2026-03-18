package com.muhammadullah.ci_debugger.pipeline.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ProcessingJob} persistence operations.
 *
 * <p>All custom queries in this repository must include a {@code status} filter.
 * The {@code processing_job} table accumulates historical rows (COMPLETED, FAILED)
 * over time — querying without a status filter will return stale rows alongside
 * active ones, which is almost never the intended behaviour.
 */
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    /**
     * Returns all jobs that are eligible to be picked up by the scheduler.
     *
     * @param now the current timestamp to evaluate scheduling and retry windows against
     * @return a list of eligible jobs, or an empty list if none are ready
     */
    @Query("""
            SELECT j FROM ProcessingJob j
            WHERE j.status IN ('PENDING')
            AND j.scheduledAt <= :now
            AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
            """)
    List<ProcessingJob> findEligibleJobs(@Param("now") Instant now);


    /**
     * Finds an active (PENDING or IN_PROGRESS) job for the given run and type.
     */
    @Query("""
            SELECT job FROM ProcessingJob job
            WHERE job.pipelineRun.id = :pipelineRunId
            AND job.jobType = :jobType
            AND job.status IN ('PENDING', 'IN_PROGRESS')
            """)
    Optional<ProcessingJob> findActiveJobByRunIdAndType(
            @Param("pipelineRunId") UUID pipelineRunId,
            @Param("jobType") ProcessingJobType jobType
    );
}