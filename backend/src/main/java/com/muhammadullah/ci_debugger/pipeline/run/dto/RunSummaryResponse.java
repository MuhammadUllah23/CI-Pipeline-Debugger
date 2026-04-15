package com.muhammadullah.ci_debugger.pipeline.run.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;

import java.time.Instant;
import java.util.UUID;

public class RunSummaryResponse {

    private UUID id;
    private PipelineRunProvider provider;
    private String owner;
    private String repo;
    private String workflowName;
    private PipelineRunStatus status;
    private PipelineRunConclusion conclusion;
    private String branch;
    private String headSha;
    private Instant startedAt;
    private Instant completedAt;
    private Long totalDurationMs;
    private Instant createdAt;

    public static RunSummaryResponse from(PipelineRun run) {
        RunSummaryResponse r = new RunSummaryResponse();
        r.id = run.getId();
        r.provider = run.getProvider();
        r.owner = run.getOwner();
        r.repo = run.getRepo();
        r.workflowName = run.getWorkflowName();
        r.status = run.getStatus();
        r.conclusion = run.getConclusion();
        r.branch = run.getBranch();
        r.headSha = run.getHeadSha();
        r.startedAt = run.getStartedAt();
        r.completedAt = run.getCompletedAt();
        r.totalDurationMs = run.getTotalDurationMs();
        r.createdAt = run.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public PipelineRunProvider getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getWorkflowName() { return workflowName; }
    public PipelineRunStatus getStatus() { return status; }
    public PipelineRunConclusion getConclusion() { return conclusion; }
    public String getBranch() { return branch; }
    public String getHeadSha() { return headSha; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getTotalDurationMs() { return totalDurationMs; }
    public Instant getCreatedAt() { return createdAt; }
}