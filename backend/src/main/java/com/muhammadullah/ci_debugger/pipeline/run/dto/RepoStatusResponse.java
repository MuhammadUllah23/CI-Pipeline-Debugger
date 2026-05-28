package com.muhammadullah.ci_debugger.pipeline.run.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.RepoHealthSummary;

public class RepoStatusResponse {

    private String owner;
    private String repo;
    private PipelineRunConclusion overallConclusion;

    public static RepoStatusResponse from(RepoHealthSummary summary) {
        RepoStatusResponse r = new RepoStatusResponse();
        r.owner = summary.getOwner();
        r.repo = summary.getRepo();
        r.overallConclusion = summary.getOverallConclusion() != null
                ? PipelineRunConclusion.valueOf(summary.getOverallConclusion())
                : null;
        return r;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public PipelineRunConclusion getOverallConclusion() {
        return overallConclusion;
    }
}
