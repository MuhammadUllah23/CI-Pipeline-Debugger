package com.muhammadullah.ci_debugger.pipeline.run.dto;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;

import java.time.Instant;
import java.util.List;

public class CommitRunSetResponse {

    private String headSha;
    private Instant startedAt;
    private List<RunSummaryResponse> runs;

    public static CommitRunSetResponse from(String headSha, Instant startedAt, List<PipelineRun> runs) {
        CommitRunSetResponse r = new CommitRunSetResponse();
        r.headSha = headSha;
        r.startedAt = startedAt;
        r.runs = runs.stream().map(RunSummaryResponse::from).toList();
        return r;
    }

    public String getHeadSha() {
        return headSha;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<RunSummaryResponse> getRuns() {
        return runs;
    }
}
