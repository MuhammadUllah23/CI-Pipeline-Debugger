package com.muhammadullah.ci_debugger.pipeline.pullrequest.dto;

import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;

import java.time.Instant;
import java.util.UUID;

public class PullRequestSummaryResponse {

    private UUID id;
    private String owner;
    private String repo;
    private int prNumber;
    private String title;
    private String headBranch;
    private PullRequestState prState;
    private Instant createdAt;
    private Instant updatedAt;

    public static PullRequestSummaryResponse from(PullRequest pr) {
        PullRequestSummaryResponse r = new PullRequestSummaryResponse();
        r.id = pr.getId();
        r.owner = pr.getOwner();
        r.repo = pr.getRepo();
        r.prNumber = pr.getPrNumber();
        r.title = pr.getTitle();
        r.headBranch = pr.getHeadBranch();
        r.prState = pr.getPrState();
        r.createdAt = pr.getCreatedAt();
        r.updatedAt = pr.getUpdatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public PullRequestState getPrState() {
        return prState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
