package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestResponse;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestSummaryResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pull-requests")
public class PullRequestController {

    private static final Logger log = LoggerFactory.getLogger(PullRequestController.class);

    private final PullRequestService pullRequestService;

    public PullRequestController(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @GetMapping("/open")
    public ResponseEntity<List<PullRequestResponse>> listOpen() {
        log.info("GET /api/pull-requests — fetching open PRs with latest run");
        return ResponseEntity.ok(pullRequestService.listOpenWithLatestRun());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PullRequestResponse> findById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page) {
        log.info("GET /api/pull-requests/{} page={}", id, page);
        return ResponseEntity.ok(pullRequestService.findById(id, page));
    }

    @GetMapping("/{owner}/{repo}")
    public ResponseEntity<Page<PullRequestSummaryResponse>> listByRepo(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "open") String status,
            @RequestParam(defaultValue = "0") int page) {
        if (!status.equals("open") && !status.equals("merged")) {
            log.warn("GET /api/pull-requests/{}/{} — invalid status param: {}", owner, repo, status);
            return ResponseEntity.badRequest().build();
        }

        PullRequestState state = status.equals("open") ? PullRequestState.OPEN : PullRequestState.MERGED;

        log.info("GET /api/pull-requests/{}/{} status={} page={}", owner, repo, status, page);
        return ResponseEntity.ok(pullRequestService.listByRepo(owner, repo, state, page));
    }
}
