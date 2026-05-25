package com.muhammadullah.ci_debugger.pipeline.run.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobService;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.job.dto.ProcessingJobResponse;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestService;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunService;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final HmacVerifier hmacVerifier;
    private final ObjectMapper objectMapper;
    private final PipelineRunService pipelineRunService;
    private final ProcessingJobService processingJobService;
    private final PullRequestService pullRequestService;

    public GitHubWebhookController(
            HmacVerifier hmacVerifier,
            ObjectMapper objectMapper,
            PipelineRunService pipelineRunService,
            ProcessingJobService processingJobService,
            PullRequestService pullRequestService) {
        this.hmacVerifier = hmacVerifier;
        this.objectMapper = objectMapper;
        this.pipelineRunService = pipelineRunService;
        this.processingJobService = processingJobService;
        this.pullRequestService = pullRequestService;
    }

    @PostMapping
    public ResponseEntity<?> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = GitHubWebhookConstants.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String eventType) throws IOException {
        if (!hmacVerifier.verify(rawBody, signature)) {
            log.warn("Rejected GitHub webhook — invalid HMAC signature");
            return ResponseEntity.status(401).build();
        }

        if (GitHubWebhookConstants.EVENT_WORKFLOW_RUN.equals(eventType)) {
            return handleWorkflowRun(rawBody);
        }

        if (GitHubWebhookConstants.EVENT_PULL_REQUEST.equals(eventType)) {
            return handlePullRequest(rawBody);
        }

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<PipelineRunResponse> handleWorkflowRun(byte[] rawBody) throws IOException {
        GitHubWebhookPayload payload = objectMapper.readValue(rawBody, GitHubWebhookPayload.class);
        PipelineRunUpsertRequest pipelineRunUpsertRequest = GitHubWebhookMapper.toUpsertRequest(payload);
        PipelineRunResponse pipelineRunResponse = pipelineRunService.upsert(pipelineRunUpsertRequest);

        log.info("Upserted pipeline run {} for {}/{} (action={})",
                pipelineRunResponse.getId(), pipelineRunResponse.getOwner(),
                pipelineRunResponse.getRepo(), payload.getAction());

        List<GitHubWebhookPayload.WorkflowRun.PullRequestRef> pullRequests = payload.getWorkflowRun().getPullRequests();

        if (pullRequests != null && !pullRequests.isEmpty()) {
            int prNumber = pullRequests.get(0).getNumber();
            PullRequest pullRequest = pullRequestService.findOrCreate(
                    GitHubWebhookConstants.PROVIDER,
                    pipelineRunResponse.getOwner(),
                    pipelineRunResponse.getRepo(),
                    prNumber);
            pipelineRunService.linkPullRequest(pipelineRunResponse.getId(), pullRequest);

            ProcessingJobResponse prJob = processingJobService.enqueue(
                    pipelineRunResponse.getId(), ProcessingJobType.GITHUB_FETCH_PR_DETAILS);
            log.info("Enqueued FETCH_PR_DETAILS job {} for pipeline run {}",
                    prJob.getId(), pipelineRunResponse.getId());
        }

        if (GitHubWebhookConstants.ACTION_COMPLETED.equals(payload.getAction())) {
            ProcessingJobResponse job = processingJobService.enqueue(
                    pipelineRunResponse.getId(), ProcessingJobType.GITHUB_FETCH_STEPS);
            log.info("Enqueued FETCH_STEPS job {} for pipeline run {}",
                    job.getId(), pipelineRunResponse.getId());
        }

        return ResponseEntity.ok(pipelineRunResponse);
    }

    private ResponseEntity<Void> handlePullRequest(byte[] rawBody) throws IOException {
        GitHubPullRequestWebhookPayload payload = objectMapper
                .readValue(rawBody, GitHubPullRequestWebhookPayload.class);

        if (GitHubWebhookConstants.ACTION_CLOSED.equals(payload.getAction())) {
            GitHubPullRequestWebhookPayload.PullRequest pr = payload.getPullRequest();
            GitHubPullRequestWebhookPayload.Repository repo = payload.getRepository();

            pullRequestService.updateState(
                    GitHubWebhookConstants.PROVIDER,
                    repo.getOwner().getLogin(),
                    repo.getName(),
                    pr.getNumber(),
                    pr.getMergedAt());

            log.info("Updated PR state for {}/{} #{} mergedAt={}",
                    repo.getOwner().getLogin(), repo.getName(),
                    pr.getNumber(), pr.getMergedAt());
        }

        return ResponseEntity.noContent().build();
    }
}
