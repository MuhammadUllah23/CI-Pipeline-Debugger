package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GitHubStepsApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubStepsApiClient.class);
    private static final String JOBS_PATH = "/repos/{owner}/{repo}/actions/runs/{runId}/jobs";

    private final RestClient gitHubRestClient;

    public GitHubStepsApiClient(RestClient gitHubRestClient) {
        this.gitHubRestClient = gitHubRestClient;
    }

    /**
     * Fetches all jobs and their steps for a given workflow run from the GitHub API
     * and maps them to a flat list of {@link PipelineStepSaveRequest}s.
     *
     * @param owner the repository owner (user or organisation)
     * @param repo  the repository name
     * @param runId the GitHub workflow run ID
     * @return a flat list of step save requests across all jobs in the run
     * @throws ServiceException with {@link ErrorCode#PROVIDER_MAPPING_FAILED} if
     *                          GitHub returns a 4xx error
     * @throws ServiceException with {@link ErrorCode#DB_CONNECTION_FAILED} if
     *                          GitHub returns a 5xx error or the request times out
     */
    public List<PipelineStepSaveRequest> fetchSteps(String owner, String repo, String runId) {
        log.info("Fetching steps from GitHub for {}/{} runId={}", owner, repo, runId);

        GitHubStepsApiResponse response = GitHubApiErrorHandler.execute(
                () -> gitHubRestClient.get()
                        .uri(JOBS_PATH, owner, repo, runId)
                        .retrieve()
                        .body(GitHubStepsApiResponse.class),
                owner, repo, runId
        );

        if (response == null || response.getJobs() == null) {
            log.warn("GitHub returned empty response for {}/{} runId={}", owner, repo, runId);
            return List.of();
        }

        List<PipelineStepSaveRequest> requests = GitHubStepsApiMapper.toSaveRequests(response);
        log.info("Fetched {} steps across {} jobs for {}/{} runId={}",
                requests.size(), response.getJobs().size(), owner, repo, runId);

        return requests;
    }
}