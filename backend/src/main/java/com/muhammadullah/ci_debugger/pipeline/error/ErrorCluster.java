package com.muhammadullah.ci_debugger.pipeline.error;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_cluster")
public class ErrorCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fingerprint", nullable = false, unique = true, length = 128)
    private String fingerprint;

    @Column(name = "owner", length = 200)
    private String owner;

    @Column(name = "repo", length = 200)
    private String repo;

    @Column(name = "job_name", length = 300)
    private String jobName;

    @Column(name = "step_name", length = 300)
    private String stepName;

    @Column(name = "conclusion", length = 30)
    private String conclusion;

    @Column(name = "representative_message", columnDefinition = "text")
    private String representativeMessage;

    @Column(name = "occurrence_count", nullable = false)
    private long occurrenceCount;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ErrorCluster() {}

    public ErrorCluster(
            String fingerprint,
            String owner,
            String repo,
            String jobName,
            String stepName,
            String conclusion
    ) {
        this.fingerprint = fingerprint;
        this.owner = owner;
        this.repo = repo;
        this.jobName = jobName;
        this.stepName = stepName;
        this.conclusion = conclusion;
        this.occurrenceCount = 1;
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    // ── lifecycle method ───────────────────────────────────────────────────

    /**
     * Records a new occurrence of this cluster — increments the count
     * and advances {@code lastSeenAt} to now.
     */
    public void recordOccurrence() {
        this.occurrenceCount++;
        this.lastSeenAt = Instant.now();
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