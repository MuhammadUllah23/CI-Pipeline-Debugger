package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.hamcrest.Matchers.containsString;

class GitHubStepsApiClientTest {

    private static final String OWNER = "owner";
    private static final String REPO = "ci-pipeline-debugger";
    private static final String RUN_ID = "123456789";
    private static final String EXPECTED_PATH = "/repos/" + OWNER + "/" + REPO + "/actions/runs/" + RUN_ID + "/jobs";

    private GitHubStepsApiClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        client = new GitHubStepsApiClient(restClient);
    }

    @Test
    void fetchSteps_happyPath_returnsMappedSteps() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "name": "build",
                      "status": "completed",
                      "conclusion": "success",
                      "steps": [
                        {
                          "name": "Checkout code",
                          "status": "completed",
                          "conclusion": "success",
                          "number": 1,
                          "started_at": "2024-01-01T00:00:00Z",
                          "completed_at": "2024-01-01T00:00:05Z"
                        },
                        {
                          "name": "Run tests",
                          "status": "completed",
                          "conclusion": "success",
                          "number": 2,
                          "started_at": "2024-01-01T00:00:05Z",
                          "completed_at": "2024-01-01T00:00:30Z"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<PipelineStepSaveRequest> steps = client.fetchSteps(OWNER, REPO, RUN_ID);

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getStepName()).isEqualTo("Checkout code");
        assertThat(steps.get(0).getJobName()).isEqualTo("build");
        assertThat(steps.get(0).getStepIndex()).isEqualTo(1);
        assertThat(steps.get(1).getStepName()).isEqualTo("Run tests");
        assertThat(steps.get(1).getStepIndex()).isEqualTo(2);
        mockServer.verify();
    }

    @Test
    void fetchSteps_multipleJobs_returnsStepsFromAllJobs() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "name": "build",
                      "status": "completed",
                      "conclusion": "success",
                      "steps": [
                        {
                          "name": "Checkout code",
                          "status": "completed",
                          "conclusion": "success",
                          "number": 1,
                          "started_at": "2024-01-01T00:00:00Z",
                          "completed_at": "2024-01-01T00:00:05Z"
                        }
                      ]
                    },
                    {
                      "name": "test",
                      "status": "completed",
                      "conclusion": "success",
                      "steps": [
                        {
                          "name": "Run unit tests",
                          "status": "completed",
                          "conclusion": "success",
                          "number": 1,
                          "started_at": "2024-01-01T00:00:10Z",
                          "completed_at": "2024-01-01T00:00:40Z"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<PipelineStepSaveRequest> steps = client.fetchSteps(OWNER, REPO, RUN_ID);

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getJobName()).isEqualTo("build");
        assertThat(steps.get(1).getJobName()).isEqualTo("test");
        mockServer.verify();
    }

    @Test
    void fetchSteps_emptyJobsList_returnsEmptyList() {
        String jsonResponse = """
                {
                  "jobs": []
                }
                """;

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<PipelineStepSaveRequest> steps = client.fetchSteps(OWNER, REPO, RUN_ID);

        assertThat(steps).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchSteps_nullResponseBody_returnsEmptyList() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        List<PipelineStepSaveRequest> steps = client.fetchSteps(OWNER, REPO, RUN_ID);

        assertThat(steps).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchSteps_fourxxError_throwsProviderApiClientError() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchSteps(OWNER, REPO, RUN_ID))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_CLIENT_ERROR);
                    assertThat(serviceException.getDetails()).containsKey("owner");
                    assertThat(serviceException.getDetails()).containsKey("repo");
                    assertThat(serviceException.getDetails()).containsKey("runId");
                    assertThat(serviceException.getDetails()).containsKey("httpStatus");
                });

        mockServer.verify();
    }

    @Test
    void fetchSteps_fivexxError_throwsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.fetchSteps(OWNER, REPO, RUN_ID))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(serviceException.getDetails()).containsKey("httpStatus");
                });

        mockServer.verify();
    }

    @Test
    void fetchSteps_timeout_throwsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new java.io.IOException("connection timed out")));

        assertThatThrownBy(() -> client.fetchSteps(OWNER, REPO, RUN_ID))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(serviceException.getDetails()).containsKey("cause");
                });

        mockServer.verify();
    }
}