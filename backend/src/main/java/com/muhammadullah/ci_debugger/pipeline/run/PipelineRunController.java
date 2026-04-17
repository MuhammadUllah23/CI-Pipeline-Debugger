package com.muhammadullah.ci_debugger.pipeline.run;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoSummaryResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RunSummaryResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/runs")
public class PipelineRunController {
    private final PipelineRunService pipelineRunService;

    public PipelineRunController(PipelineRunService pipelineRunService) {
        this.pipelineRunService = pipelineRunService;
    }

    /**
     * Returns all repos grouped by owner, repo, workflowName,
     * with the 5 most recent runs per workflow.
     */
    @GetMapping()
    public List<RepoSummaryResponse> listGrouped() {
        return pipelineRunService.listGrouped();
    }


    /**
     * Returns a paginated list of runs for a specific repo, sorted by
     * {@code createdAt DESC}. Defaults to page 0 if not specified.
     */
    @GetMapping("/{owner}/{repo}")
    public Page<RunSummaryResponse> listByRepo(
        @PathVariable String owner,
        @PathVariable String repo,
        @RequestParam(defaultValue = "0") int page ) {
        return pipelineRunService.listByRepo(owner, repo, page);
    }
    
    /**
     * Returns a single pipeline run by ID.
     */
    @GetMapping("/{id}")
    public PipelineRunResponse findById(@PathVariable UUID id) {
        return pipelineRunService.findById(id);
    }
    

}
