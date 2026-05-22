package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestRepository;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client.GitHubPullRequestApiClient;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client.GitHubPullRequestApiResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GitHubFetchPrDetailsJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubFetchPrDetailsJobHandler.class);

    private final GitHubPullRequestApiClient gitHubPullRequestApiClient;
    private final PullRequestRepository pullRequestRepository;
    private final PipelineRunRepository pipelineRunRepository;

    public GitHubFetchPrDetailsJobHandler(
            GitHubPullRequestApiClient gitHubPullRequestApiClient,
            PullRequestRepository pullRequestRepository,
            PipelineRunRepository pipelineRunRepository) {
        this.gitHubPullRequestApiClient = gitHubPullRequestApiClient;
        this.pullRequestRepository = pullRequestRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @Override
    public ProcessingJobType getJobType() {
        return ProcessingJobType.GITHUB_FETCH_PR_DETAILS;
    }

    @Override
    @Transactional
    public void handle(ProcessingJob job) {
        PipelineRun run = pipelineRunRepository.findById(job.getPipelineRun().getId())
                .orElseThrow(() -> ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                        .addDetail("pipelineRunId", job.getPipelineRun().getId()));
        PullRequest pullRequest = run.getPullRequest();

        log.info("Fetching PR details for {}/{} prNumber={} pipelineRun={}",
                run.getOwner(), run.getRepo(), pullRequest.getPrNumber(), run.getId());

        if (pullRequest.getPrState() != null) {
            log.info("PR {} already has details — skipping API call", pullRequest.getId());
            return;
        }

        GitHubPullRequestApiResponse response = gitHubPullRequestApiClient
                .fetchPullRequest(run.getOwner(), run.getRepo(), pullRequest.getPrNumber());

        PullRequestState state = GitHubPullRequestApiClient.resolveState(response);

        pullRequest.applyDetails(
                response.getTitle(),
                response.getHead().getSha(),
                response.getHead().getRef(),
                response.getState(),
                state);

        pullRequestRepository.save(pullRequest);
        log.info("Applied details to PR {} ({}/{} #{}) state={}",
                pullRequest.getId(), run.getOwner(), run.getRepo(),
                pullRequest.getPrNumber(), state);
    }
}
