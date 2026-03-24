package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
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

    public GitHubFetchStepsJobHandler(
            GitHubStepsApiClient gitHubStepsApiClient,
            PipelineStepService pipelineStepService
    ) {
        this.gitHubStepsApiClient = gitHubStepsApiClient;
        this.pipelineStepService = pipelineStepService;
    }

    @Override
    public ProcessingJobType getJobType() {
        return ProcessingJobType.FETCH_STEPS;
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
    }
}