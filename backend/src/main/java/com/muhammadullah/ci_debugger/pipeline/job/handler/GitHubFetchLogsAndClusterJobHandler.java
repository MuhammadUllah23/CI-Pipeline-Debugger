package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.pipeline.error.ErrorIngestionService;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.github.client.GitHubLogsApiClient;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubFetchLogsAndClusterJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubFetchLogsAndClusterJobHandler.class);

    private final GitHubLogsApiClient gitHubLogsApiClient;
    private final PipelineStepService pipelineStepService;
    private final ErrorIngestionService errorIngestionService;

    public GitHubFetchLogsAndClusterJobHandler(
            GitHubLogsApiClient gitHubLogsApiClient,
            PipelineStepService pipelineStepService,
            ErrorIngestionService errorIngestionService
    ) {
        this.gitHubLogsApiClient = gitHubLogsApiClient;
        this.pipelineStepService = pipelineStepService;
        this.errorIngestionService = errorIngestionService;
    }

    @Override
    public ProcessingJobType getJobType() {
        return ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER;
    }

    @Override
    public void handle(ProcessingJob job) {
        PipelineRun run = job.getPipelineRun();

        log.info("Fetching logs and clustering errors for pipeline run {} ({}/{})",
                run.getId(), run.getOwner(), run.getRepo());

        List<PipelineStep> failedSteps = pipelineStepService.findFailedSteps(run.getId());

        if (failedSteps.isEmpty()) {
            log.info("No failed steps found for pipeline run {} — skipping log fetch", run.getId());
            return;
        }

        Map<String, List<String>> errorLinesByJob = gitHubLogsApiClient.fetchErrorLines(
                run.getOwner(),
                run.getRepo(),
                run.getProviderRunId()
        );

        Map<PipelineStep, String> snippetByStep = buildSnippetByStep(failedSteps, errorLinesByJob);

        errorIngestionService.ingestErrors(run.getId(), snippetByStep);

        log.info("Completed log fetch and clustering for pipeline run {}", run.getId());
    }

    /**
     * Matches each failed step to its job's extracted error lines and builds
     * a snippet string. Steps with no matching error lines get a null snippet —
     * they are still passed to the ingestion service so the failure is recorded.
     */
    private Map<PipelineStep, String> buildSnippetByStep(
            List<PipelineStep> failedSteps,
            Map<String, List<String>> errorLinesByJob
    ) {
        Map<PipelineStep, String> snippetByStep = new HashMap<>();

        for (PipelineStep step : failedSteps) {
            List<String> errorLines = errorLinesByJob.get(step.getJobName());
            String snippet = buildSnippet(errorLines);
            snippetByStep.put(step, snippet);
        }

        return snippetByStep;
    }

    /**
     * Joins extracted error lines into a single snippet string, or returns
     * {@code null} if no error lines were found for this step's job.
     */
    private String buildSnippet(List<String> errorLines) {
        if (errorLines == null || errorLines.isEmpty()) {
            return null;
        }
        return String.join("\n", errorLines);
    }
}