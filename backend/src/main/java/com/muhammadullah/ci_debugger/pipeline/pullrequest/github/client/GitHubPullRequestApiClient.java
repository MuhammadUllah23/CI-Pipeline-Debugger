package com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client;

import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;
import com.muhammadullah.ci_debugger.pipeline.run.github.client.GitHubApiErrorHandler;
import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubPullRequestApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubPullRequestApiClient.class);
    private static final String PR_PATH = "/repos/{owner}/{repo}/pulls/{prNumber}";

    private final RestClient gitHubRestClient;

    public GitHubPullRequestApiClient(RestClient gitHubRestClient) {
        this.gitHubRestClient = gitHubRestClient;
    }

    /**
     * Fetches pull request details for a given PR number from the GitHub API.
     *
     * @param owner    the repository owner
     * @param repo     the repository name
     * @param prNumber the pull request number
     * @return the pull request API response
     */
    public GitHubPullRequestApiResponse fetchPullRequest(String owner, String repo, int prNumber) {
        log.info("Fetching PR details from GitHub for {}/{} prNumber={}", owner, repo, prNumber);

        GitHubPullRequestApiResponse response = GitHubApiErrorHandler.execute(
                () -> gitHubRestClient.get()
                        .uri(PR_PATH, owner, repo, prNumber)
                        .retrieve()
                        .body(GitHubPullRequestApiResponse.class),
                owner, repo, String.valueOf(prNumber)
        );

        if (response == null) {
            log.warn("GitHub returned empty response for {}/{} prNumber={}", owner, repo, prNumber);
            throw ServiceException.of(ErrorCode.PROVIDER_API_CLIENT_ERROR)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("prNumber", prNumber);
        }

        log.info("Fetched PR details for {}/{} prNumber={} state={} mergedAt={}",
                owner, repo, prNumber, response.getState(), response.getMergedAt());

        return response;
    }

    /**
     * Maps a GitHub API response to a {@link PullRequestState}.
     * Merged takes precedence — a PR with a non-null {@code mergedAt}
     * is always {@code MERGED} regardless of the raw state value.
     *
     * @param response the GitHub pull request API response
     * @return the normalized pull request state
     */
    public static PullRequestState resolveState(GitHubPullRequestApiResponse response) {
        if (response.getMergedAt() != null) {
            return PullRequestState.MERGED;
        }
        return switch (response.getState().toLowerCase()) {
            case "open" -> PullRequestState.OPEN;
            case "closed" -> PullRequestState.CLOSED;
            default -> PullRequestState.CLOSED;
        };
    }
}
