package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubLogsApiClientTest {

    private static final String OWNER = "mhu-ventures";
    private static final String REPO = "ci-pipeline-debugger";
    private static final String RUN_ID = "123456789";
    private static final String EXPECTED_PATH = "/repos/" + OWNER + "/" + REPO + "/actions/runs/" + RUN_ID + "/logs";

    private GitHubLogsApiClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        client = new GitHubLogsApiClient(restClient);
    }

    @Test
    void fetchErrorLines_happyPath_returnsErrorLinesMappedByJobName() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z [INFO] Scanning for projects...
                2026-04-01T02:31:11.379Z [ERROR] No POM in this directory
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).containsKey("build");
        assertThat(result.get("build")).hasSize(2);
        assertThat(result.get("build").get(0)).isEqualTo("[ERROR] No POM in this directory");
        assertThat(result.get("build").get(1)).isEqualTo("##[error]Process completed with exit code 1.");
        mockServer.verify();
    }

    @Test
    void fetchErrorLines_multipleJobFiles_returnsErrorLinesFromAllJobs() throws IOException {
        String buildLog = """
                2026-04-01T02:31:11.379Z [ERROR] Build failed
                """;
        String testLog = """
                2026-04-01T02:31:11.379Z [ERROR] Tests failed
                """;

        byte[] zipBytes = buildZip(Map.of(
                "0_build.txt", buildLog,
                "1_test.txt", testLog
        ));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).containsKeys("build", "test");
        assertThat(result.get("build").get(0)).isEqualTo("[ERROR] Build failed");
        assertThat(result.get("test").get(0)).isEqualTo("[ERROR] Tests failed");
        mockServer.verify();
    }

    @Test
    void fetchErrorLines_systemFilesSkipped_notIncludedInResult() throws IOException {
        String systemLog = """
                2026-04-01T02:31:11.379Z [ERROR] Some system error
                """;

        byte[] zipBytes = buildZip(Map.of("build/system.txt", systemLog));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchErrorLines_jobWithNoErrorLines_excludedFromResult() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z [INFO] Build successful
                2026-04-01T02:31:11.379Z [INFO] All tests passed
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchErrorLines_emptyResponseBody_returnsEmptyMap() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[0], MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchErrorLines_fourxxError_throwsProviderApiClientError() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchErrorLines(OWNER, REPO, RUN_ID))
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
    void fetchErrorLines_fivexxError_throwsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.fetchErrorLines(OWNER, REPO, RUN_ID))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(se.getDetails()).containsKey("httpStatus");
                });

        mockServer.verify();
    }

    @Test
    void fetchErrorLines_timeout_throwsProviderApiUnavailable() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new java.io.IOException("connection timed out")));

        assertThatThrownBy(() -> client.fetchErrorLines(OWNER, REPO, RUN_ID))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE);
                    assertThat(se.getDetails()).containsKey("cause");
                });

        mockServer.verify();
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private byte[] buildZip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}