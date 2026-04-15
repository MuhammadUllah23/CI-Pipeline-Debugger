package com.muhammadullah.ci_debugger.pipeline.run.dto;

import java.util.List;

public class WorkflowSummaryResponse {

    private String workflowName;
    private List<RunSummaryResponse> recentRuns;

    public WorkflowSummaryResponse(String workflowName, List<RunSummaryResponse> recentRuns) {
        this.workflowName = workflowName;
        this.recentRuns = recentRuns;
    }

    public String getWorkflowName() { return workflowName; }
    public List<RunSummaryResponse> getRecentRuns() { return recentRuns; }
}