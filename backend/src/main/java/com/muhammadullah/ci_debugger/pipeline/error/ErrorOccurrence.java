package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_occurrence")
// Note: a partial unique constraint on (error_cluster_id, pipeline_run_id, pipeline_step_id)
// WHERE pipeline_step_id IS NOT NULL is enforced at the database level via V4 
public class ErrorOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_cluster_id", nullable = false)
    private ErrorCluster errorCluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_run_id", nullable = false)
    private PipelineRun pipelineRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_step_id")
    private PipelineStep pipelineStep;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "snippet", columnDefinition = "text")
    private String snippet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ErrorOccurrence() {}

    public ErrorOccurrence(ErrorCluster errorCluster, PipelineRun pipelineRun, PipelineStep pipelineStep) {
        this.errorCluster = errorCluster;
        this.pipelineRun = pipelineRun;
        this.pipelineStep = pipelineStep;
    }

    public UUID getId() { return id; }
    public ErrorCluster getErrorCluster() { return errorCluster; }
    public PipelineRun getPipelineRun() { return pipelineRun; }
    public PipelineStep getPipelineStep() { return pipelineStep; }
    public Integer getLineNumber() { return lineNumber; }
    public String getSnippet() { return snippet; }
    public Instant getCreatedAt() { return createdAt; }
}