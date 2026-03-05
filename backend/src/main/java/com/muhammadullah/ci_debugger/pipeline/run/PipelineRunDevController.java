package com.muhammadullah.ci_debugger.pipeline.run;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@Profile("dev")
@RequestMapping("/api/runs/dev")
public class PipelineRunDevController {

    private final PipelineRunRepository repository;

    public PipelineRunDevController(PipelineRunRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/seed")
    public PipelineRun seed() {
        long providerRunId = System.currentTimeMillis(); 

        PipelineRun run = new PipelineRun(
                "GITHUB",
                "mhu-ventures",
                "ci-pipeline-debugger",
                providerRunId,
                PipelineRunStatus.QUEUED
        );

        run.setWorkflowName("CI Pipeline Debugger - Seed");
        run.setBranch("main");
        run.setHeadSha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); // 40 chars ok
        // Optional: simulate started
        run.markStarted(Instant.now());

        return repository.save(run);
    }

    @GetMapping
    public List<PipelineRun> listRuns() {
        return repository.findAll();
    }
}
