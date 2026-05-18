package com.muhammadullah.ci_debugger.pipeline.job.handler;

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
        PipelineRun run = job.getPipelineRun();
        int prNumber = run.getPrNumber();

        log.info("Fetching PR details for {}/{} prNumber={} pipelineRun={}",
                run.getOwner(), run.getRepo(), prNumber, run.getId());

        PullRequest pullRequest = pullRequestRepository
                .findByProviderAndOwnerAndRepoAndPrNumber(
                        run.getProvider().name(),
                        run.getOwner(),
                        run.getRepo(),
                        prNumber)
                .orElseGet(() -> fetchAndPersist(run, prNumber));

        run.setPullRequest(pullRequest);
        pipelineRunRepository.save(run);

        log.info("Linked PR {} to pipeline run {}", pullRequest.getId(), run.getId());
    }

    private PullRequest fetchAndPersist(PipelineRun run, int prNumber) {
        GitHubPullRequestApiResponse response = gitHubPullRequestApiClient
                .fetchPullRequest(run.getOwner(), run.getRepo(), prNumber);

        PullRequestState state = GitHubPullRequestApiClient.resolveState(response);

        PullRequest pullRequest = new PullRequest(
                run.getProvider().name(),
                run.getOwner(),
                run.getRepo(),
                prNumber);

        pullRequest.applyDetails(
                response.getTitle(),
                response.getHead().getSha(),
                response.getHead().getRef(),
                response.getState(),
                state);

        PullRequest saved = pullRequestRepository.save(pullRequest);
        log.info("Persisted new PR {} ({}/{} #{}) state={}",
                saved.getId(), run.getOwner(), run.getRepo(), prNumber, state);
        return saved;
    }
}
