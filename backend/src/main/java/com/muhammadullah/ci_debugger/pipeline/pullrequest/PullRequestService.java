package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.run.dto.CommitRunSetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PullRequestService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestService.class);
    private static final int PR_RUNS_PAGE_SIZE = 20;
    private static final int COMMIT_SET_PAGE_SIZE = 10;

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
                .collect(Collectors.groupingBy(run -> run.getPullRequest().getId()))
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
     * Returns a paginated list of commit run sets for a pull request.
     * Each set groups all workflow runs triggered by the same commit.
     *
     * @param prId the pull request ID
     * @param page zero-based page number
     * @return a page of commit run sets ordered by most recent commit first
     */
    @Transactional(readOnly = true)
    public Page<CommitRunSetResponse> listRunSets(UUID prId, int page) {
        Page<String> headShas = pipelineRunRepository.findDistinctHeadShasByPrId(
                prId, PageRequest.of(page, COMMIT_SET_PAGE_SIZE));

        if (headShas.isEmpty()) {
            return Page.empty();
        }

        List<PipelineRun> runs = pipelineRunRepository.findByPullRequestIdAndHeadShaIn(
                prId, headShas.getContent());

        Map<String, List<PipelineRun>> grouped = runs.stream()
                .collect(Collectors.groupingBy(PipelineRun::getHeadSha));

        List<CommitRunSetResponse> sets = headShas.getContent().stream()
                .map(sha -> {
                    List<PipelineRun> shaRuns = grouped.getOrDefault(sha, List.of());
                    Instant startedAt = shaRuns.stream()
                            .map(PipelineRun::getStartedAt)
                            .filter(t -> t != null)
                            .min(Instant::compareTo)
                            .orElse(null);
                    return CommitRunSetResponse.from(sha, startedAt, shaRuns);
                })
                .toList();

        return new PageImpl<>(sets, PageRequest.of(page, COMMIT_SET_PAGE_SIZE), headShas.getTotalElements());
    }

    /**
     * Finds an existing pull request by provider identity or creates a minimal
     * row with just the identity fields if one does not exist.
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

    /**
     * Returns a paginated list of pull requests for a given repo filtered by state,
     * each with their latest run per workflow.
     *
     * @param owner the repository owner
     * @param repo  the repository name
     * @param state the PR state to filter by
     * @param page  zero-based page number
     * @return a page of pull request responses with runs
     */
    @Transactional(readOnly = true)
    public Page<PullRequestResponse> listByRepo(String owner, String repo, PullRequestState state, int page) {
        Page<PullRequest> prs = pullRequestRepository.findByOwnerAndRepoAndPrStateOrderByUpdatedAtDesc(
                owner, repo, state, PageRequest.of(page, PR_RUNS_PAGE_SIZE));

        if (prs.isEmpty()) {
            return Page.empty();
        }

        List<UUID> prIds = prs.getContent().stream()
                .map(PullRequest::getId)
                .toList();

        List<PipelineRun> runs = pipelineRunRepository.findLatestRunPerWorkflowForPrIds(prIds);

        Map<UUID, List<PipelineRun>> runsByPrId = runs.stream()
                .collect(Collectors.groupingBy(r -> r.getPullRequest().getId()));

        List<PullRequestResponse> responses = prs.getContent().stream()
                .map(pr -> PullRequestResponse.from(pr, runsByPrId.getOrDefault(pr.getId(), List.of())))
                .toList();

        return new PageImpl<>(responses, PageRequest.of(page, PR_RUNS_PAGE_SIZE), prs.getTotalElements());
    }
}
