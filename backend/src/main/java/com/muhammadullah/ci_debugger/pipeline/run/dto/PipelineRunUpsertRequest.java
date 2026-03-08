package com.muhammadullah.ci_debugger.pipeline.run.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class PipelineRunUpsertRequest {

    @NotBlank
    private String provider; 

    @NotBlank
    private String owner;

    @NotBlank
    private String repo;

    @NotNull
    private Long providerRunId;

    @NotBlank
    private String status;

    private String workflowName;
    private String conclusion; 
    private String headSha;
    private String branch;

    private Instant startedAt;
    private Instant completedAt;
    private Long totalDurationMs;

    public String getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public Long getProviderRunId() { return providerRunId; }
    public String getStatus() { return status; }
    public String getWorkflowName() { return workflowName; }
    public String getConclusion() { return conclusion; }
    public String getHeadSha() { return headSha; }
    public String getBranch() { return branch; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getTotalDurationMs() { return totalDurationMs; }

    public void setProvider(String provider) { this.provider = provider; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setRepo(String repo) { this.repo = repo; }
    public void setProviderRunId(Long providerRunId) { this.providerRunId = providerRunId; }
    public void setStatus(String status) { this.status = status; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setTotalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
}
