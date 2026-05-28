package com.muhammadullah.ci_debugger.pipeline.run;

import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private static final Logger log = LoggerFactory.getLogger(RepoController.class);

    private final PipelineRunService pipelineRunService;

    public RepoController(PipelineRunService pipelineRunService) {
        this.pipelineRunService = pipelineRunService;
    }

    @GetMapping
    public ResponseEntity<List<RepoStatusResponse>> listRepoStatuses() {
        log.info("GET /api/repos — fetching repo health summaries");
        return ResponseEntity.ok(pipelineRunService.listRepoStatuses());
    }
}
