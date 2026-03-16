package com.muhammadullah.ci_debugger.pipeline.run.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunService;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;

import java.io.IOException;

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

    public GitHubWebhookController(
            HmacVerifier hmacVerifier,
            ObjectMapper objectMapper,
            PipelineRunService pipelineRunService
    ) {
        this.hmacVerifier = hmacVerifier;
        this.objectMapper = objectMapper;
        this.pipelineRunService = pipelineRunService;
    }

    @PostMapping
    public ResponseEntity<PipelineRunResponse> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = GitHubWebhookConstants.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String eventType
    ) throws IOException {
        if (!hmacVerifier.verify(rawBody, signature)) {
            log.warn("Rejected GitHub webhook — invalid HMAC signature");
            return ResponseEntity.status(401).build();
        }

        if (!GitHubWebhookConstants.EVENT_WORKFLOW_RUN.equals(eventType)) {
            return ResponseEntity.noContent().build();
        }

        GitHubWebhookPayload payload = objectMapper.readValue(rawBody, GitHubWebhookPayload.class);
        PipelineRunUpsertRequest req = GitHubWebhookMapper.toUpsertRequest(payload);
        PipelineRunResponse response = pipelineRunService.upsert(req);

        if (GitHubWebhookConstants.ACTION_COMPLETED.equals(payload.getAction())) {
            // TODO: enqueue FETCH_STEPS job for providerRunId = response.getProviderRunId()
            log.debug("Pipeline run {} completed — step fetch job not yet implemented", response.getProviderRunId());
        }

        return ResponseEntity.ok(response);
    }
}