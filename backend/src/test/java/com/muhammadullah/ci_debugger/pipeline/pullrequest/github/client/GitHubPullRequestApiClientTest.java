package com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubPullRequestApiClientTest {

    private static final String OWNER = "owner";
    private static final String REPO = "repo";
    private static final int PR_NUMBER = 42;
    private static final String EXPECTED_PATH = "/repos/" + OWNER + "/" + REPO + "/pulls/" + PR_NUMBER;

    private GitHubPullRequestApiClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        client = new GitHubPullRequestApiClient(restClient);
    }

    @Test
    void fetchPullRequestOpenPrReturnsMappedResponse() {
        String jsonResponse = """
                {
                  "number": 42,
                  "title": "Add feature",
                  "state": "open",
                  "merged_at": null,
                  "head": {
                    "sha": "abc123",
                    "ref": "feature-branch"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubPullRequestApiResponse response = client.fetchPullRequest(OWNER, REPO, PR_NUMBER);

        assertThat(response.getNumber()).isEqualTo(42);
        assertThat(response.getTitle()).isEqualTo("Add feature");
        assertThat(response.getState()).isEqualTo("open");
        assertThat(response.getMergedAt()).isNull();
        assertThat(response.getHead().getSha()).isEqualTo("abc123");
        assertThat(response.getHead().getRef()).isEqualTo("feature-branch");
        mockServer.verify();
    }

    @Test
    void fetchPullRequestMergedPrReturnsMergedAtTimestamp() {
        String jsonResponse = """
                {
                  "number": 42,
                  "title": "Add feature",
                  "state": "closed",
                  "merged_at": "2024-01-01T00:00:00Z",
                  "head": {
                    "sha": "abc123",
                    "ref": "feature-branch"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubPullRequestApiResponse response = client.fetchPullRequest(OWNER, REPO, PR_NUMBER);

        assertThat(response.getMergedAt()).isNotNull();
        assertThat(response.getState()).isEqualTo("closed");
        mockServer.verify();
    }

    @Test
    void resolveStateMergedAtNotNullReturnsMerged() {
        String jsonResponse = """
                {
                  "number": 42,
                  "title": "Add feature",
                  "state": "closed",
                  "merged_at": "2024-01-01T00:00:00Z",
                  "head": {
                    "sha": "abc123",
                    "ref": "feature-branch"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubPullRequestApiResponse response = client.fetchPullRequest(OWNER, REPO, PR_NUMBER);

        assertThat(GitHubPullRequestApiClient.resolveState(response)).isEqualTo(PullRequestState.MERGED);
        mockServer.verify();
    }

    @Test
    void resolveStateOpenPrReturnsOpen() {
        String jsonResponse = """
                {
                  "number": 42,
                  "title": "Add feature",
                  "state": "open",
                  "merged_at": null,
                  "head": {
                    "sha": "abc123",
                    "ref": "feature-branch"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubPullRequestApiResponse response = client.fetchPullRequest(OWNER, REPO, PR_NUMBER);

        assertThat(GitHubPullRequestApiClient.resolveState(response)).isEqualTo(PullRequestState.OPEN);
        mockServer.verify();
    }

    @Test
    void resolveStateClosedPrReturnsClosed() {
        String jsonResponse = """
                {
                  "number": 42,
                  "title": "Add feature",
                  "state": "closed",
                  "merged_at": null,
                  "head": {
                    "sha": "abc123",
                    "ref": "feature-branch"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubPullRequestApiResponse response = client.fetchPullRequest(OWNER, REPO, PR_NUMBER);

        assertThat(GitHubPullRequestApiClient.resolveState(response)).isEqualTo(PullRequestState.CLOSED);
        mockServer.verify();
    }

    @Test
    void fetchPullRequestFourxxErrorThrowsProviderApiClientError() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchPullRequest(OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_CLIENT_ERROR);
                    assertThat(se.getDetails()).containsKey("owner");
                    assertThat(se.getDetails()).containsKey("repo");
                    assertThat(se.getDetails()).containsKey("runId");
                    assertThat(se.getDetails()).containsKey("httpStatus");
                });

        mockServer.verify();
    }

    @Test
    void fetchPullRequestFivexxErrorThrowsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.fetchPullRequest(OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(se.getDetails()).containsKey("httpStatus");
                });

        mockServer.verify();
    }

    @Test
    void fetchPullRequestTimeoutThrowsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new java.io.IOException("connection timed out")));

        assertThatThrownBy(() -> client.fetchPullRequest(OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(se.getDetails()).containsKey("cause");
                });

        mockServer.verify();
    }
}
