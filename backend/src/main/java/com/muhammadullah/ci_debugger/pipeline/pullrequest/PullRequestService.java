package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            PipelineRunRepository pipelineRunRepository
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    /**
     * Returns all open pull requests with their latest pipeline run each.
     * Used by the dashboard to show open PR CI status at a glance.
     *
     * @return list of open PRs each with their most recent run
     */
    @Transactional(readOnly = true)
    public List<PullRequestResponse> listOpenWithLatestRun() {
        List<PipelineRun> runs = pipelineRunRepository.findLatestRunForOpenPullRequests();

        return runs.stream()
                .map(run -> PullRequestResponse.from(run.getPullRequest(), List.of(run)))
                .toList();
    }

    /**
     * Returns a single pull request by ID with a paginated list of its runs.
     * Used for the PR detail page.
     *
     * @param id   the pull request ID
     * @param page zero-based page number
     * @return the pull request with paginated runs
     * @throws ServiceException with {@link ErrorCode#DB_RECORD_NOT_FOUND} if not found
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
}
