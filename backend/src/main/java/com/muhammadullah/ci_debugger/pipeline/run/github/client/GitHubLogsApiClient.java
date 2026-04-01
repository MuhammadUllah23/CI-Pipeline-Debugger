package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class GitHubLogsApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubLogsApiClient.class);
    private static final String LOGS_PATH = "/repos/{owner}/{repo}/actions/runs/{runId}/logs";

    private final RestClient gitHubRestClient;

    public GitHubLogsApiClient(RestClient gitHubRestClient) {
        this.gitHubRestClient = gitHubRestClient;
    }

    /**
     * Fetches the log zip for a workflow run from the GitHub API and extracts
     * error lines per job.
     *
     * <p>Only files matching the pattern {@code {index}_{jobName}.txt} at the
     * root level of the zip are processed — system-level files like
     * {@code build/system.txt} are ignored. Error lines are identified by the
     * prefixes {@code [ERROR]} and {@code ##[error]}.
     *
     * @param owner the repository owner
     * @param repo  the repository name
     * @param runId the GitHub workflow run ID
     * @return a map of job name to extracted error lines, empty if no errors found
     * @throws ServiceException with {@link ErrorCode#PROVIDER_API_CLIENT_ERROR} if
     *                          GitHub returns a 4xx error
     * @throws ServiceException with {@link ErrorCode#PROVIDER_API_UNAVAILABLE} if
     *                          GitHub returns a 5xx error or the request times out
     * @throws ServiceException with {@link ErrorCode#PROVIDER_MAPPING_FAILED} if
     *                          the zip cannot be parsed
     */
    public Map<String, List<String>> fetchErrorLines(String owner, String repo, String runId) {
        log.info("Fetching logs from GitHub for {}/{} runId={}", owner, repo, runId);

        byte[] zipBytes = GitHubApiErrorHandler.execute(
                () -> gitHubRestClient.get()
                        .uri(LOGS_PATH, owner, repo, runId)
                        .retrieve()
                        .body(byte[].class),
                owner, repo, runId
        );

        if (zipBytes == null || zipBytes.length == 0) {
            log.warn("GitHub returned empty log zip for {}/{} runId={}", owner, repo, runId);
            return Map.of();
        }

        try {
            Map<String, List<String>> errorLinesByJob = extractErrorLines(zipBytes, owner, repo, runId);
            log.info("Extracted error lines from {} job log(s) for {}/{} runId={}",
                    errorLinesByJob.size(), owner, repo, runId);
            return errorLinesByJob;

        } catch (IOException e) {
            log.error("Failed to parse log zip for {}/{} runId={} — {}", owner, repo, runId, e.getMessage());
            throw ServiceException.of(ErrorCode.PROVIDER_MAPPING_FAILED)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("runId", runId)
                    .addDetail("cause", e.getMessage());
        }
    }

    /**
     * Unzips the raw bytes and extracts error lines from each job log file.
     * Files not matching the {@code {index}_{jobName}.txt} pattern are skipped.
     */
    private Map<String, List<String>> extractErrorLines(byte[] zipBytes, String owner, String repo, String runId)
            throws IOException {
        Map<String, List<String>> errorLinesByJob = new HashMap<>();

        // ByteArrayInputStream wraps the byte array so it can be read as a stream. 
        // ZipInputStream wraps that so we can iterate through the zip entries one by one.

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String jobName = parseJobName(entry.getName());

                if (jobName == null) {
                    log.debug("Skipping zip entry {} for {}/{} runId={}", entry.getName(), owner, repo, runId);
                    zipInputStream.closeEntry();
                    continue;
                }

                List<String> errorLines = extractErrorLinesFromEntry(zipInputStream);

                if (!errorLines.isEmpty()) {
                    errorLinesByJob.put(jobName, errorLines);
                }

                zipInputStream.closeEntry();
            }
        }

        return errorLinesByJob;
    }

    /**
     * Parses the job name from a zip entry filename.
     * Expects the pattern {@code {index}_{jobName}.txt} at the root level.
     * Returns {@code null} for system files or nested paths like
     * {@code build/system.txt}.
     */
    private String parseJobName(String entryName) {
        if (entryName.contains("/")) {
            return null;
        }

        if (!entryName.endsWith(".txt")) {
            return null;
        }

        int underscoreIndex = entryName.indexOf('_');
        if (underscoreIndex < 0) {
            return null;
        }

        String prefix = entryName.substring(0, underscoreIndex);
        if (!prefix.chars().allMatch(Character::isDigit)) {
            return null;
        }

        return entryName.substring(underscoreIndex + 1, entryName.length() - ".txt".length());
    }

    /**
     * Reads lines from the current zip entry and returns those that start with
     * {@code [ERROR]} or {@code ##[error]} after stripping the timestamp prefix.
     */
    private List<String> extractErrorLinesFromEntry(ZipInputStream zipInputStream) throws IOException {
        List<String> errorLines = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            String stripped = stripTimestamp(line);
            if (stripped.startsWith("[ERROR]") || stripped.startsWith("##[error]")) {
                errorLines.add(stripped);
            }
        }

        return errorLines;
    }

    /**
     * Strips the leading timestamp from a log line.
     * Lines follow the format {@code 2026-04-01T02:31:11.379Z content}.
     * Returns the original line if no timestamp is found.
     */
    private String stripTimestamp(String line) {
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < line.length() - 1) {
            return line.substring(spaceIndex + 1);
        }
        return line;
    }
}