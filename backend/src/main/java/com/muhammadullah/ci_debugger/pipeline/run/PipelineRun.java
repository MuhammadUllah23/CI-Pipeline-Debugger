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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PipelineRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusion", length = 30)
    private PipelineRunConclusion conclusion;

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

    public PipelineRun(String provider, String owner, String repo, Long providerRunId, PipelineRunStatus status) {
        this.provider = (provider == null || provider.isBlank()) ? "GITHUB" : provider;
        this.owner = owner;
        this.repo = repo;
        this.providerRunId = providerRunId;
        this.status = (status == null) ? PipelineRunStatus.UNKNOWN : status;
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public Long getProviderRunId() { return providerRunId; }
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


    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }
    public void setBranch(String branch) { this.branch = branch; }

    // --- lifecycle methods ---

    public void markStarted(Instant startedAt) {
        this.startedAt = startedAt;
        this.status = PipelineRunStatus.IN_PROGRESS;
    }

    public void markCompleted(PipelineRunConclusion conclusion, Instant completedAt, long durationMs) {
        this.conclusion = (conclusion == null) ? PipelineRunConclusion.UNKNOWN : conclusion;
        this.completedAt = completedAt;
        this.totalDurationMs = durationMs;
        this.status = PipelineRunStatus.COMPLETED;
    }
}