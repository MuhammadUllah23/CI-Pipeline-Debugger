package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    private static final String OWNER = "owner";
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
    @DisplayName("returns group header and meaningful error lines for known tools")
    void fetchErrorLinesReturnsGroupContextAndErrorLine() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z shell: /usr/bin/bash -e {0}
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [INFO] Scanning for projects...
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn clean compile",
                "[ERROR] No POM in this directory",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.startsWith("[INFO]"));
        mockServer.verify();
    }

    @Test
    @DisplayName("filters out shell declarations empty lines and command echoes")
    void fetchErrorLinesFiltersNoiseLines() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z shell: /usr/bin/bash -e {0}
                2026-04-01T02:31:11.379Z [command]/usr/bin/git version
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [INFO] Scanning for projects...
                2026-04-01T02:31:11.379Z [ERROR] No POM in this directory
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn clean compile",
                "[ERROR] No POM in this directory",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.startsWith("shell:"));
        assertThat(result.get("build")).noneMatch(line -> line.startsWith("[command]"));
        assertThat(result.get("build")).noneMatch(line -> line.startsWith("[INFO]"));
        mockServer.verify();
    }

    @Test
    @DisplayName("stops at first ##[error] line and does not capture subsequent ones")
    void fetchErrorLinesStopsAtFirstErrorLine() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] First error
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                2026-04-01T02:31:11.379Z ##[error]Another error occurred.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn clean compile",
                "[ERROR] First error",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.contains("Another error occurred"));
        mockServer.verify();
    }

    @Test
    @DisplayName("only captures group containing the error when multiple groups present")
    void fetchErrorLinesOnlyCapturesGroupContainingError() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Checkout code
                2026-04-01T02:31:11.379Z actions/checkout@v4
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [INFO] Checking out repository
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] No POM in this directory
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn clean compile",
                "[ERROR] No POM in this directory",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.contains("Checkout code"));
        assertThat(result.get("build")).noneMatch(line -> line.startsWith("[INFO]"));
        mockServer.verify();
    }

    @Test
    @DisplayName("excludes ##[endgroup] lines from snippet")
    void fetchErrorLinesExcludesEndGroupLines() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] No POM in this directory
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).noneMatch(line -> line.startsWith("##[endgroup]"));
        mockServer.verify();
    }

    @Test
    @DisplayName("returns snippets from all jobs when multiple job files present")
    void fetchErrorLinesReturnsSnippetsFromAllJobs() throws IOException {
        String buildLog = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] Build failed
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;
        String testLog = """
                2026-04-01T02:31:11.379Z ##[group]Run tests
                2026-04-01T02:31:11.379Z npm test
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] Tests failed
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of(
                "0_build.txt", buildLog,
                "1_test.txt", testLog));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).containsKeys("build", "test");
        assertThat(result.get("build")).contains("[ERROR] Build failed");
        assertThat(result.get("test")).contains("[ERROR] Tests failed");
        mockServer.verify();
    }

    @Test
    @DisplayName("excludes job from result when no ##[error] line found")
    void fetchErrorLinesExcludesJobWithNoErrorLine() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [INFO] Build successful
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
    @DisplayName("skips system files and does not include them in result")
    void fetchErrorLinesSkipsSystemFiles() throws IOException {
        String systemLog = """
                2026-04-01T02:31:11.379Z ##[group]System
                2026-04-01T02:31:11.379Z [ERROR] Some system error
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
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
    @DisplayName("returns empty map when response body is empty")
    void fetchErrorLinesReturnsEmptyMapForEmptyResponse() {
        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[0], MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("throws PROVIDER_API_CLIENT_ERROR on 4xx response")
    void fetchErrorLinesThrowsProviderApiClientErrorOnFourxx() {
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
    @DisplayName("throws PROVIDER_API_UNAVAILABLE on 5xx response")
    void fetchErrorLinesThrowsProviderApiUnavailableOnFivexx() {
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
    @DisplayName("throws PROVIDER_API_UNAVAILABLE on timeout")
    void fetchErrorLinesThrowsProviderApiUnavailableOnTimeout() {
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

    @Test
    @DisplayName("does not include lines after ##[error] in snippet")
    void fetchErrorLinesDoesNotIncludePostErrorLines() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn clean compile
                2026-04-01T02:31:11.379Z mvn clean compile
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] No POM in this directory
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                2026-04-01T02:31:11.379Z Post job cleanup.
                2026-04-01T02:31:11.379Z [command]/usr/bin/git version
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn clean compile",
                "[ERROR] No POM in this directory",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.contains("Post job cleanup"));
        assertThat(result.get("build")).noneMatch(line -> line.contains("git version"));
        mockServer.verify();
    }

    @Test
    @DisplayName("strips ANSI escape codes from snippet lines")
    void fetchErrorLinesStripsAnsiCodesFromSnippet() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run npm run format:check
                2026-04-01T02:31:11.379Z \u001B[36;1mnpm run format:check\u001B[0m
                2026-04-01T02:31:11.379Z [[33mwarn[39m] Code style issues found in 16 files.
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_frontend.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("frontend")).noneMatch(line -> line.contains("\u001B"));
        assertThat(result.get("frontend")).noneMatch(line -> line.contains("[33m"));
        assertThat(result.get("frontend")).noneMatch(line -> line.contains("[39m"));
        assertThat(result.get("frontend")).anyMatch(line -> line.contains("Code style issues found in 16 files."));
        mockServer.verify();
    }

    @Test
    @DisplayName("preserves error lines when post job cleanup group appears before ##[error]")
    void fetchErrorLinesPreservesErrorLinesWhenPostJobCleanupGroupAppears() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run mvn test
                2026-04-01T02:31:11.379Z mvn test
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z [ERROR] Tests run: 14, Failures: 3, Errors: 0
                2026-04-01T02:31:11.379Z [ERROR] Failed to execute goal
                2026-04-01T02:31:11.379Z ##[group]Post job cleanup
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_build.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("build")).containsExactly(
                "##[group]Run mvn test",
                "[ERROR] Tests run: 14, Failures: 3, Errors: 0",
                "[ERROR] Failed to execute goal",
                "##[error]Process completed with exit code 1.");
        assertThat(result.get("build")).noneMatch(line -> line.contains("Post job cleanup"));
        mockServer.verify();
    }

    @Test
    @DisplayName("falls back to line limit for unknown tools with no recognized prefixes")
    void fetchErrorLinesFallsBackToLineLimitForUnknownTool() throws IOException {
        String logContent = """
                2026-04-01T02:31:11.379Z ##[group]Run npm run build
                2026-04-01T02:31:11.379Z npm run build
                2026-04-01T02:31:11.379Z ##[endgroup]
                2026-04-01T02:31:11.379Z building client environment...
                2026-04-01T02:31:11.379Z transforming modules...
                2026-04-01T02:31:11.379Z build failed in 409ms
                2026-04-01T02:31:11.379Z could not resolve './RunSet.jsx'
                2026-04-01T02:31:11.379Z module not found
                2026-04-01T02:31:11.379Z ##[error]Process completed with exit code 1.
                """;

        byte[] zipBytes = buildZip(Map.of("0_frontend.txt", logContent));

        mockServer.expect(requestTo(containsString(EXPECTED_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(zipBytes, MediaType.APPLICATION_OCTET_STREAM));

        Map<String, List<String>> result = client.fetchErrorLines(OWNER, REPO, RUN_ID);

        assertThat(result.get("frontend")).contains("##[group]Run npm run build");
        assertThat(result.get("frontend")).contains("build failed in 409ms");
        assertThat(result.get("frontend")).contains("could not resolve './RunSet.jsx'");
        assertThat(result.get("frontend")).contains("##[error]Process completed with exit code 1.");
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
