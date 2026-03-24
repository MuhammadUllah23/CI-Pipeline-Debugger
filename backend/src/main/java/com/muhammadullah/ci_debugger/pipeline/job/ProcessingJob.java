package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "processing_job"
    // Note: a partial unique constraint on (pipeline_run_id, job_type)
    // WHERE status IN ('PENDING', 'IN_PROGRESS') is enforced at the database
    // level via V9__add_active_job_unique_constraint.sql — JPA does not support
    // partial unique constraints so it cannot be expressed here.
)
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_run_id", nullable = false)
    private PipelineRun pipelineRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private ProcessingJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingJobStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProcessingJob() {}

    public ProcessingJob(PipelineRun pipelineRun, ProcessingJobType jobType) {
        this.pipelineRun = pipelineRun;
        this.jobType = jobType;
        this.status = ProcessingJobStatus.PENDING;
        this.scheduledAt = Instant.now();
    }

    // ── lifecycle methods ──────────────────────────────────────────────────

    /**
     * Transitions the job to {@link ProcessingJobStatus#IN_PROGRESS} and
     * records the start time. Should be called by the scheduler immediately
     * before handing the job off to a handler.
     */
    public void markInProgress() {
        this.status = ProcessingJobStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    /**
     * Transitions the job to {@link ProcessingJobStatus#COMPLETED} and
     * records the completion time.
     */
    public void markCompleted() {
        this.status = ProcessingJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Records a failed attempt and either re-queues the job for retry or
     * transitions it to {@link ProcessingJobStatus#FAILED} if all attempts
     * are exhausted.
     *
     * <p>If {@code attempts < maxAttempts} after incrementing, the job returns
     * to {@link ProcessingJobStatus#PENDING} with {@code nextRetryAt} set to
     * the provided retry time. Otherwise the job is permanently marked as
     * {@link ProcessingJobStatus#FAILED}.
     *
     * @param error        the error message to record in {@code lastError}
     * @param nextRetryAt  the earliest time the job should be retried;
     *                     ignored if all attempts are exhausted
     */
    public void markFailed(String error, Instant nextRetryAt) {
        this.attempts++;
        this.lastError = error;

        if (this.attempts >= this.maxAttempts) {
            this.status = ProcessingJobStatus.FAILED;
        } else {
            this.status = ProcessingJobStatus.PENDING;
            this.nextRetryAt = nextRetryAt;
        }
    }

    /**
     * Permanently fails the job immediately regardless of remaining attempts.
     * Used for unrecoverable errors where retrying would never succeed —
     * for example when no handler is registered for the job type.
     *
     * @param error the error message to record in {@code lastError}
     */
    public void markFailedImmediately(String error) {
        this.attempts = this.maxAttempts;
        this.lastError = error;
        this.status = ProcessingJobStatus.FAILED;
    }

    public UUID getId() { return id; }
    public PipelineRun getPipelineRun() { return pipelineRun; }
    public ProcessingJobType getJobType() { return jobType; }
    public ProcessingJobStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}