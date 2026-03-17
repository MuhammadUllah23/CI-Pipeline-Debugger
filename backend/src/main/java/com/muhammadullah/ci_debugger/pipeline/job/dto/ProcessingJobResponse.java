package com.muhammadullah.ci_debugger.pipeline.job.dto;

import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobStatus;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;

import java.time.Instant;
import java.util.UUID;

public class ProcessingJobResponse {

    private UUID id;
    private UUID pipelineRunId;
    private ProcessingJobType jobType;
    private ProcessingJobStatus status;
    private int attempts;
    private int maxAttempts;
    private String lastError;
    private Instant scheduledAt;
    private Instant nextRetryAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProcessingJobResponse from(ProcessingJob job) {
        ProcessingJobResponse r = new ProcessingJobResponse();
        r.id = job.getId();
        r.pipelineRunId = job.getPipelineRun().getId();
        r.jobType = job.getJobType();
        r.status = job.getStatus();
        r.attempts = job.getAttempts();
        r.maxAttempts = job.getMaxAttempts();
        r.lastError = job.getLastError();
        r.scheduledAt = job.getScheduledAt();
        r.nextRetryAt = job.getNextRetryAt();
        r.startedAt = job.getStartedAt();
        r.completedAt = job.getCompletedAt();
        r.createdAt = job.getCreatedAt();
        r.updatedAt = job.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getPipelineRunId() { return pipelineRunId; }
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