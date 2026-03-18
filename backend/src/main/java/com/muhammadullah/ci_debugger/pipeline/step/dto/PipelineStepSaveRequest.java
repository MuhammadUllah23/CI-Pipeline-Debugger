package com.muhammadullah.ci_debugger.pipeline.step.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class PipelineStepSaveRequest {

    @NotBlank
    private String jobName;

    @NotBlank
    private String stepName;

    @NotNull
    private Integer stepIndex;

    private PipelineRunStatus status;
    private PipelineRunConclusion conclusion;

    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;

    public String getJobName() { return jobName; }
    public String getStepName() { return stepName; }
    public Integer getStepIndex() { return stepIndex; }
    public PipelineRunStatus getStatus() { return status; }
    public PipelineRunConclusion getConclusion() { return conclusion; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getDurationMs() { return durationMs; }

    public void setJobName(String jobName) { this.jobName = jobName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public void setStepIndex(Integer stepIndex) { this.stepIndex = stepIndex; }
    public void setStatus(PipelineRunStatus status) { this.status = status; }
    public void setConclusion(PipelineRunConclusion conclusion) { this.conclusion = conclusion; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}