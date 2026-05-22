package com.muhammadullah.ci_debugger.pipeline.pullrequest.dto;

import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RunSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PullRequestResponse {

    private UUID id;
    private String provider;
    private String owner;
    private String repo;
    private int prNumber;
    private String title;
    private String headSha;
    private String headBranch;
    private String prStateRaw;
    private PullRequestState prState;
    private List<RunSummaryResponse> runs;
    private Instant createdAt;
    private Instant updatedAt;

    public static PullRequestResponse from(PullRequest pr, List<PipelineRun> runs) {
        PullRequestResponse response = new PullRequestResponse();
        response.id = pr.getId();
        response.provider = pr.getProvider();
        response.owner = pr.getOwner();
        response.repo = pr.getRepo();
        response.prNumber = pr.getPrNumber();
        response.title = pr.getTitle();
        response.headSha = pr.getHeadSha();
        response.headBranch = pr.getHeadBranch();
        response.prStateRaw = pr.getPrStateRaw();
        response.prState = pr.getPrState();
        response.runs = runs.stream().map(RunSummaryResponse::from).toList();
        response.createdAt = pr.getCreatedAt();
        response.updatedAt = pr.getUpdatedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
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

    public String getHeadSha() {
        return headSha;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public String getPrStateRaw() {
        return prStateRaw;
    }

    public PullRequestState getPrState() {
        return prState;
    }

    public List<RunSummaryResponse> getRuns() {
        return runs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
