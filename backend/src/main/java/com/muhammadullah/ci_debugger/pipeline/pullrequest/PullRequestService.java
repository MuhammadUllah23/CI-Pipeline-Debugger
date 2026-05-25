package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PullRequestService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestService.class);
    private static final int PR_RUNS_PAGE_SIZE = 20;

    private final PullRequestRepository pullRequestRepository;
    private final PipelineRunRepository pipelineRunRepository;

    public PullRequestService(
            PullRequestRepository pullRequestRepository,
            PipelineRunRepository pipelineRunRepository) {
        this.pullRequestRepository = pullRequestRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    /**
     * Returns all open pull requests with their latest pipeline run each.
     *
     * @return list of open PRs each with their most recent run
     */
    @Transactional(readOnly = true)
    public List<PullRequestResponse> listOpenWithLatestRun() {
        List<PipelineRun> runs = pipelineRunRepository.findLatestRunPerWorkflowForOpenPullRequests();

        return runs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        run -> run.getPullRequest().getId()))
                .values()
                .stream()
                .map(prRuns -> PullRequestResponse.from(prRuns.get(0).getPullRequest(), prRuns))
                .toList();
    }

    /**
     * Returns a single pull request by ID with a paginated list of its runs.
     *
     * @param id   the pull request ID
     * @param page zero-based page number
     * @return the pull request with paginated runs
     * @throws ServiceException with {@link ErrorCode#DB_RECORD_NOT_FOUND} if not
     *                          found
     */
    @Transactional(readOnly = true)
    public PullRequestResponse findById(UUID id, int page) {
        PullRequest pr = pullRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pull request {} not found", id);
                    return ServiceException.of(ErrorCode.DB_RECORD_NOT_FOUND)
                            .addDetail("pullRequestId", id);
                });

        Page<PipelineRun> runs = pipelineRunRepository
                .findByPullRequestIdOrderByCreatedAtDesc(id, PageRequest.of(page, PR_RUNS_PAGE_SIZE));

        return PullRequestResponse.from(pr, runs.getContent());
    }

    /**
     * Finds an existing pull request by provider identity or creates a minimal
     * row with just the identity fields if one does not exist.
     *
     * @param provider the CI provider (e.g. "GITHUB")
     * @param owner    the repository owner
     * @param repo     the repository name
     * @param prNumber the pull request number
     * @return the existing or newly created pull request
     */
    public PullRequest findOrCreate(String provider, String owner, String repo, int prNumber) {
        return pullRequestRepository
                .findByProviderAndOwnerAndRepoAndPrNumber(provider, owner, repo, prNumber)
                .orElseGet(() -> {
                    try {
                        log.info("Creating minimal PR row for {}/{} prNumber={}", owner, repo, prNumber);
                        PullRequest pr = new PullRequest(provider, owner, repo, prNumber);
                        return pullRequestRepository.save(pr);
                    } catch (DataIntegrityViolationException e) {
                        log.info("PR row already exists for {}/{} prNumber={} — concurrent insert, fetching existing",
                                owner, repo, prNumber);
                        return pullRequestRepository
                                .findByProviderAndOwnerAndRepoAndPrNumber(provider, owner, repo, prNumber)
                                .orElseThrow(() -> ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                                        .addDetail("owner", owner)
                                        .addDetail("repo", repo)
                                        .addDetail("prNumber", prNumber));
                    }
                });
    }

    /**
     * Updates the state of a pull request when it is closed or merged.
     *
     * @param provider the CI provider
     * @param owner    the repository owner
     * @param repo     the repository name
     * @param prNumber the pull request number
     * @param mergedAt the merge timestamp, or {@code null} if the PR was closed
     *                 without merging
     */
    @Transactional
    public void updateState(String provider, String owner, String repo, int prNumber, Instant mergedAt) {
        pullRequestRepository
                .findByProviderAndOwnerAndRepoAndPrNumber(provider, owner, repo, prNumber)
                .ifPresent(pr -> {
                    PullRequestState newState = mergedAt != null ? PullRequestState.MERGED : PullRequestState.CLOSED;
                    String rawState = mergedAt != null ? "merged" : "closed";
                    pr.applyDetails(pr.getTitle(), pr.getHeadSha(), pr.getHeadBranch(), rawState, newState);
                    pullRequestRepository.save(pr);
                    log.info("Updated PR {}/{} #{} state to {}", owner, repo, prNumber, newState);
                });
    }
}
