package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobService;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.github.client.GitHubStepsApiClient;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStepService;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GitHubFetchStepsJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubFetchStepsJobHandler.class);

    private final GitHubStepsApiClient gitHubStepsApiClient;
    private final PipelineStepService pipelineStepService;
    private final ProcessingJobService processingJobService;

    public GitHubFetchStepsJobHandler(
            GitHubStepsApiClient gitHubStepsApiClient,
            PipelineStepService pipelineStepService,
            ProcessingJobService processingJobService
    ) {
        this.gitHubStepsApiClient = gitHubStepsApiClient;
        this.pipelineStepService = pipelineStepService;
        this.processingJobService = processingJobService;
    }

    @Override
    public ProcessingJobType getJobType() {
        return ProcessingJobType.GITHUB_FETCH_STEPS;
    }

    @Override
    public void handle(ProcessingJob job) {
        PipelineRun run = job.getPipelineRun();

        log.info("Fetching steps for pipeline run {} ({}/{})",
                run.getId(), run.getOwner(), run.getRepo());

        List<PipelineStepSaveRequest> stepRequests = gitHubStepsApiClient.fetchSteps(
                run.getOwner(),
                run.getRepo(),
                run.getProviderRunId()
        );

        pipelineStepService.saveAll(run.getId(), stepRequests);

        log.info("Saved {} steps for pipeline run {}", stepRequests.size(), run.getId());

        if (run.getConclusion() != PipelineRunConclusion.SUCCESS) {
            processingJobService.enqueue(run.getId(), ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER);
            log.info("Enqueued GITHUB_FETCH_LOGS_AND_CLUSTER job for pipeline run {}", run.getId());
        } else {
            log.info("Skipping log fetch for successful pipeline run {}", run.getId());
        }
    }
}