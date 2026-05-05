package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name="pull_request",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_pull_request",
            columnNames = {"provider", "owner", "repo", "pr_number"}
        )
    }
)
public class PullRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "owner", nullable = false, length = 200)
    private String owner;

    @Column(name = "repo", nullable = false, length = 200)
    private String repo;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "head_sha", length = 64)
    private String headSha;

    @Column(name = "head_branch", length = 200)
    private String headBranch;

    @Column(name = "pr_state_raw", length = 20)
    private String prStateRaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "pr_state", length = 20)
    private PullRequestState prState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PullRequest() {}

    public PullRequest(String provider, String owner, String repo, int prNumber) {
        this.provider = provider;
        this.owner = owner;
        this.repo = repo;
        this.prNumber = prNumber;
    }

    /**
     * Applies PR details fetched from the provider API.
     *
     * @param title      the PR title
     * @param headSha    the current head commit SHA on the PR branch
     * @param headBranch the PR source branch name
     * @param prStateRaw the raw state string from the provider
     * @param prState    the normalized state
     */
    public void applyDetails(
            String title,
            String headSha,
            String headBranch,
            String prStateRaw,
            PullRequestState prState
    ) {
        this.title = title;
        this.headSha = headSha;
        this.headBranch = headBranch;
        this.prStateRaw = prStateRaw;
        this.prState = prState;
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public int getPrNumber() { return prNumber; }
    public String getTitle() { return title; }
    public String getHeadSha() { return headSha; }
    public String getHeadBranch() { return headBranch; }
    public String getPrStateRaw() { return prStateRaw; }
    public PullRequestState getPrState() { return prState; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
