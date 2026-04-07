package com.muhammadullah.ci_debugger.pipeline.run;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        String providerRunId = "123456789"; 

        PipelineRun run = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
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

    @PostMapping("/{id}/touch")
    public ResponseEntity<PipelineRun> touch(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "CI Pipeline Debugger - Updated") String workflowName,
            @RequestParam(defaultValue = "main") String branch) {
        return repository.findById(id)
                .map(run -> {
                    run.setWorkflowName(workflowName);
                    run.setBranch(branch);

                    // Optional: simulate completion to test conclusion + duration + completed_at
                    if (run.getCompletedAt() == null) {
                        Instant now = Instant.now();
                        Instant started = (run.getStartedAt() != null) ? run.getStartedAt() : now.minusSeconds(5);
                        long durationMs = Math.max(0, now.toEpochMilli() - started.toEpochMilli());
                        run.markCompleted(PipelineRunConclusion.SUCCESS, now, durationMs);
                    }

                    PipelineRun saved = repository.save(run);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PipelineRun> listRuns() {
        return repository.findAll();
    }
}
