package com.muhammadullah.ci_debugger.pipeline.step;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pipeline_step",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pipeline_step",
                        columnNames = {"pipeline_run_id", "job_name", "step_index"}
                )
        }
)
public class PipelineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_run_id", nullable = false)
    private PipelineRun pipelineRun;

    @Column(name = "job_name", length = 300)
    private String jobName;

    @Column(name = "step_name", nullable = false, length = 300)
    private String stepName;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private PipelineRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusion", length = 30)
    private PipelineRunConclusion conclusion;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PipelineStep() {}

    public PipelineStep(PipelineRun pipelineRun, String jobName, String stepName, int stepIndex) {
        this.pipelineRun = pipelineRun;
        this.jobName = jobName;
        this.stepName = stepName;
        this.stepIndex = stepIndex;
    }

    // ── lifecycle method ───────────────────────────────────────────────────

    /**
     * Applies completion data to the step. Duration is calculated from
     * {@code startedAt} and {@code completedAt} if not explicitly provided.
     *
     * @param status      the final status of the step
     * @param conclusion  the final conclusion of the step
     * @param startedAt   when the step started, may be {@code null}
     * @param completedAt when the step completed, may be {@code null}
     * @param durationMs  explicit duration in milliseconds, or {@code null}
     *                    to derive it from {@code startedAt} and {@code completedAt}
     */
    public void applyCompletion(
            PipelineRunStatus status,
            PipelineRunConclusion conclusion,
            Instant startedAt,
            Instant completedAt,
            Long durationMs
    ) {
        this.status = status;
        this.conclusion = conclusion;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = deriveDuration(durationMs, startedAt, completedAt);
    }

    private long deriveDuration(Long provided, Instant startedAt, Instant completedAt) {
        if (provided != null && provided >= 0) return provided;
        if (startedAt == null || completedAt == null) return 0;
        return Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
    }

    public UUID getId() { return id; }
    public PipelineRun getPipelineRun() { return pipelineRun; }
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