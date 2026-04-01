package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

public final class GitHubApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiErrorHandler.class);

    private GitHubApiErrorHandler() {}

    /**
     * Executes a GitHub API call and maps provider-level exceptions to
     * {@link ServiceException}. Callers supply the API call as a lambda —
     * all HTTP error handling is centralized here.
     *
     * @param apiCall the GitHub API call to execute
     * @param owner   the repository owner, included in error details
     * @param repo    the repository name, included in error details
     * @param runId   the workflow run ID, included in error details
     * @return the result of the API call
     * @throws ServiceException with {@link ErrorCode#PROVIDER_API_CLIENT_ERROR} on 4xx
     * @throws ServiceException with {@link ErrorCode#PROVIDER_API_UNAVAILABLE} on 5xx or timeout
     */
    public static <T> T execute(Supplier<T> apiCall, String owner, String repo, String runId) {
        try {
            return apiCall.get();
        } catch (HttpClientErrorException e) {
            log.warn("GitHub API 4xx error for {}/{} runId={} — status={} message={}",
                    owner, repo, runId, e.getStatusCode(), e.getMessage());
            throw ServiceException.of(ErrorCode.PROVIDER_API_CLIENT_ERROR)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("runId", runId)
                    .addDetail("httpStatus", e.getStatusCode().value())
                    .addDetail("cause", e.getMessage());

        } catch (HttpServerErrorException e) {
            log.warn("GitHub API 5xx error for {}/{} runId={} — status={} message={}",
                    owner, repo, runId, e.getStatusCode(), e.getMessage());
            throw ServiceException.of(ErrorCode.PROVIDER_API_UNAVAILABLE)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("runId", runId)
                    .addDetail("httpStatus", e.getStatusCode().value())
                    .addDetail("cause", e.getMessage());

        } catch (ResourceAccessException e) {
            log.warn("GitHub API timeout or connection failure for {}/{} runId={} — {}",
                    owner, repo, runId, e.getMessage());
            throw ServiceException.of(ErrorCode.PROVIDER_API_UNAVAILABLE)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("runId", runId)
                    .addDetail("cause", e.getMessage());
        }
    }
}