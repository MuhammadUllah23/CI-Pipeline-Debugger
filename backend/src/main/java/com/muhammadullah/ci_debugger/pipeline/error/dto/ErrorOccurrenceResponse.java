package com.muhammadullah.ci_debugger.pipeline.error.dto;

import com.muhammadullah.ci_debugger.pipeline.error.ErrorOccurrence;

import java.time.Instant;
import java.util.UUID;

public class ErrorOccurrenceResponse {

    private UUID id;
    private UUID errorClusterId;
    private UUID pipelineRunId;
    private UUID pipelineStepId;
    private String snippet;
    private Integer lineNumber;
    private Instant createdAt;

    public static ErrorOccurrenceResponse from(ErrorOccurrence occurrence) {
        ErrorOccurrenceResponse r = new ErrorOccurrenceResponse();
        r.id = occurrence.getId();
        r.errorClusterId = occurrence.getErrorCluster().getId();
        r.pipelineRunId = occurrence.getPipelineRun().getId();
        r.pipelineStepId = occurrence.getPipelineStep() != null
                ? occurrence.getPipelineStep().getId()
                : null;
        r.snippet = occurrence.getSnippet();
        r.lineNumber = occurrence.getLineNumber();
        r.createdAt = occurrence.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getErrorClusterId() { return errorClusterId; }
    public UUID getPipelineRunId() { return pipelineRunId; }
    public UUID getPipelineStepId() { return pipelineStepId; }
    public String getSnippet() { return snippet; }
    public Integer getLineNumber() { return lineNumber; }
    public Instant getCreatedAt() { return createdAt; }
}