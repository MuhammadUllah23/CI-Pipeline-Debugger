package com.muhammadullah.ci_debugger.pipeline.error.dto;

import com.muhammadullah.ci_debugger.pipeline.error.ErrorCluster;

import java.time.Instant;
import java.util.UUID;

public class ErrorClusterResponse {

    private UUID id;
    private String fingerprint;
    private String owner;
    private String repo;
    private String jobName;
    private String stepName;
    private String conclusion;
    private String representativeMessage;
    private long occurrenceCount;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private Instant createdAt;

    public static ErrorClusterResponse from(ErrorCluster cluster) {
        ErrorClusterResponse r = new ErrorClusterResponse();
        r.id = cluster.getId();
        r.fingerprint = cluster.getFingerprint();
        r.owner = cluster.getOwner();
        r.repo = cluster.getRepo();
        r.jobName = cluster.getJobName();
        r.stepName = cluster.getStepName();
        r.conclusion = cluster.getConclusion();
        r.representativeMessage = cluster.getRepresentativeMessage();
        r.occurrenceCount = cluster.getOccurrenceCount();
        r.firstSeenAt = cluster.getFirstSeenAt();
        r.lastSeenAt = cluster.getLastSeenAt();
        r.createdAt = cluster.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getFingerprint() { return fingerprint; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getJobName() { return jobName; }
    public String getStepName() { return stepName; }
    public String getConclusion() { return conclusion; }
    public String getRepresentativeMessage() { return representativeMessage; }
    public long getOccurrenceCount() { return occurrenceCount; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
}