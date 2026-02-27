package com.muhammadullah.ci_debugger.pipeline.run;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "pipeline_run",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_pipeline_run",
            columnNames = {"provider", "owner", "repo", "provider_run_id"}
        )
    }
)
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider = "GITHUB";

    @Column(name = "owner", nullable = false, length = 200)
    private String owner;

    @Column(name = "repo", nullable = false, length = 200)
    private String repo;

    @Column(name = "provider_run_id", nullable = false)
    private Long providerRunId;

    @Column(name = "workflow_name", length = 300)
    private String workflowName;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "conclusion", length = 30)
    private String conclusion;

    @Column(name = "head_sha", length = 64)
    private String headSha;

    @Column(name = "branch", length = 200)
    private String branch;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PipelineRun() {
    }

    public PipelineRun(String provider,
                   String owner,
                   String repo,
                   Long providerRunId,
                   String status) {
    this.provider = provider;
    this.owner = owner;
    this.repo = repo;
    this.providerRunId = providerRunId;
    this.status = status;
}

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public Long getProviderRunId() { return providerRunId; }
    public String getWorkflowName() { return workflowName; }
    public String getStatus() { return status; }
    public String getConclusion() { return conclusion; }
    public String getHeadSha() { return headSha; }
    public String getBranch() { return branch; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getTotalDurationMs() { return totalDurationMs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setProvider(String provider) { this.provider = provider; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setRepo(String repo) { this.repo = repo; }
    public void setProviderRunId(Long providerRunId) { this.providerRunId = providerRunId; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public void setStatus(String status) { this.status = status; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setTotalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void markStarted(Instant startedAt) {
        this.startedAt = startedAt;
        this.status = "IN_PROGRESS";
    };

    public void markCompleted(String conclusion, Instant completedAt, long durationMs) {
        this.conclusion = conclusion;
        this.completedAt = completedAt;
        this.totalDurationMs = durationMs;
        this.status = "COMPLETED";
    };
}