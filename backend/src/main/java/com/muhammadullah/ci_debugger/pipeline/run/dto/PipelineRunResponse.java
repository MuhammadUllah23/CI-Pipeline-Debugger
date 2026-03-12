package com.muhammadullah.ci_debugger.pipeline.run.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;

import java.time.Instant;
import java.util.UUID;

public class PipelineRunResponse {

    private UUID id;
    private PipelineRunProvider provider;
    private String owner;
    private String repo;
    private String providerRunId;

    private String workflowName;
    private PipelineRunStatus status;
    private PipelineRunConclusion conclusion;

    private String headSha;
    private String branch;

    private Instant startedAt;
    private Instant completedAt;
    private Long totalDurationMs;

    private Instant createdAt;
    private Instant updatedAt;

    public static PipelineRunResponse from(PipelineRun run) {
        PipelineRunResponse r = new PipelineRunResponse();
        r.id = run.getId();
        r.provider = run.getProvider();
        r.owner = run.getOwner();
        r.repo = run.getRepo();
        r.providerRunId = run.getProviderRunId();
        r.workflowName = run.getWorkflowName();
        r.status = run.getStatus();
        r.conclusion = run.getConclusion();
        r.headSha = run.getHeadSha();
        r.branch = run.getBranch();
        r.startedAt = run.getStartedAt();
        r.completedAt = run.getCompletedAt();
        r.totalDurationMs = run.getTotalDurationMs();
        r.createdAt = run.getCreatedAt();
        r.updatedAt = run.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public PipelineRunProvider getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getProviderRunId() { return providerRunId; }
    public String getWorkflowName() { return workflowName; }
    public PipelineRunStatus getStatus() { return status; }
    public PipelineRunConclusion getConclusion() { return conclusion; }
    public String getHeadSha() { return headSha; }
    public String getBranch() { return branch; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getTotalDurationMs() { return totalDurationMs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
