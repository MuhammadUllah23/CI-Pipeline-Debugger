package com.muhammadullah.ci_debugger.pipeline.run.dto;

import java.util.List;

public class RepoSummaryResponse {

    private String owner;
    private String repo;
    private List<WorkflowSummaryResponse> workflows;

    public RepoSummaryResponse(String owner, String repo, List<WorkflowSummaryResponse> workflows) {
        this.owner = owner;
        this.repo = repo;
        this.workflows = workflows;
    }

    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public List<WorkflowSummaryResponse> getWorkflows() { return workflows; }
}