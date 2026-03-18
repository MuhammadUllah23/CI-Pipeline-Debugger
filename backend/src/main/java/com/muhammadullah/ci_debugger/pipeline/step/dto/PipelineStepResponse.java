package com.muhammadullah.ci_debugger.pipeline.step.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;

import java.time.Instant;
import java.util.UUID;

public class PipelineStepResponse {

    private UUID id;
    private UUID pipelineRunId;
    private String jobName;
    private String stepName;
    private int stepIndex;
    private PipelineRunStatus status;
    private PipelineRunConclusion conclusion;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Instant createdAt;

    public static PipelineStepResponse from(PipelineStep step) {
        PipelineStepResponse response = new PipelineStepResponse();
        response.id = step.getId();
        response.pipelineRunId = step.getPipelineRun().getId();
        response.jobName = step.getJobName();
        response.stepName = step.getStepName();
        response.stepIndex = step.getStepIndex();
        response.status = step.getStatus();
        response.conclusion = step.getConclusion();
        response.startedAt = step.getStartedAt();
        response.completedAt = step.getCompletedAt();
        response.durationMs = step.getDurationMs();
        response.createdAt = step.getCreatedAt();
        return response;
    }

    public UUID getId() { return id; }
    public UUID getPipelineRunId() { return pipelineRunId; }
    public String getJobName() { return jobName; }
    public String getStepName() { return stepName; }
    public int getStepIndex() { return stepIndex; }
    public PipelineRunStatus getStatus() { return status; }
    public PipelineRunConclusion getConclusion() { return conclusion; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getDurationMs() { return durationMs; }
    public Instant getCreatedAt() { return createdAt; }
}