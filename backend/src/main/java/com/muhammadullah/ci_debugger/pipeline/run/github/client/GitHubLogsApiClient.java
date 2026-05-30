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
    private static final int MAX_SNIPPET_LINES = 15;

    private final RestClient gitHubRestClient;

    public GitHubLogsApiClient(RestClient gitHubRestClient) {
        this.gitHubRestClient = gitHubRestClient;
    }

    /**
     * Fetches the log zip for a workflow run from the GitHub API and extracts
     * error lines per job.
     *
     * <p>
     * Only files matching the pattern {@code {index}_{jobName}.txt} at the
     * root level of the zip are processed — system-level files like
     * {@code build/system.txt} are ignored. The snippet for each failing step
     * is captured from the last {@code ##[group]} marker before the
     * {@code ##[error]} line, excluding metadata noise.
     *
     * <p>
     * For known tools (Maven, ESLint, Prettier), only meaningful prefixed
     * lines are included. For unknown tools, the last {@value MAX_SNIPPET_LINES}
     * lines are returned as a fallback.
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
                owner, repo, runId);

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
     * Reads lines from the current zip entry and returns a snippet for the
     * failing step.
     *
     * <p>
     * All non-noise lines within each {@code ##[group]} are accumulated in
     * {@code currentGroupLines}. When a new {@code ##[group]} is encountered,
     * the current group is saved as {@code bestCandidateLines} if it has
     * meaningful content — this handles post-job cleanup groups that appear
     * between the error output and {@code ##[error]}.
     *
     * <p>
     * When {@code ##[error]} is encountered, the snippet is built from
     * {@code currentGroupLines} if it has content, otherwise from
     * {@code bestCandidateLines}. Known tool prefixes are preferred; unknown
     * tools fall back to the last {@value MAX_SNIPPET_LINES} lines.
     *
     * <p>
     * Returns an empty list if no {@code ##[error]} line is found.
     */
    private List<String> extractErrorLinesFromEntry(ZipInputStream zipInputStream) throws IOException {
        List<String> currentGroupLines = new ArrayList<>();
        List<String> bestCandidateLines = new ArrayList<>();
        List<String> errorGroupLines = new ArrayList<>();
        boolean foundError = false;
        boolean skippedCommandEcho = true;

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            String stripped = stripAnsiCodes(stripTimestamp(line));

            if (stripped.startsWith("##[group]")) {
                if (hasMeaningfulContent(currentGroupLines)) {
                    bestCandidateLines = new ArrayList<>(currentGroupLines);
                }
                currentGroupLines = new ArrayList<>();
                currentGroupLines.add(stripped);
                skippedCommandEcho = false;
                continue;
            }

            if (stripped.startsWith("##[endgroup]")) {
                continue;
            }

            if (stripped.startsWith("##[error]")) {
                if (!foundError) {
                    List<String> snippetSource = hasMeaningfulContent(currentGroupLines)
                            ? currentGroupLines
                            : bestCandidateLines;
                    errorGroupLines = buildSnippetLines(snippetSource);
                    foundError = true;
                }
                errorGroupLines.add(stripped);
                return errorGroupLines;
            }

            if (!skippedCommandEcho) {
                skippedCommandEcho = true;
                continue;
            }

            if (!isNoiseLine(stripped)) {
                currentGroupLines.add(stripped);
            }
        }

        return errorGroupLines;
    }

    /**
     * Builds the snippet lines from the current group content.
     *
     * <p>
     * If the group contains lines matching known error prefixes, only those
     * lines are returned alongside the group header — giving a clean focused
     * snippet for known tools like Maven, ESLint, and Prettier.
     *
     * <p>
     * If no recognized prefixes are found, falls back to the last
     * {@value MAX_SNIPPET_LINES} lines — ensuring unknown tools like Vite
     * still produce useful context.
     */
    private List<String> buildSnippetLines(List<String> groupLines) {
        List<String> meaningful = groupLines.stream()
                .filter(l -> isMeaningfulLine(l) || l.startsWith("##[group]"))
                .toList();

        if (meaningful.size() > 1) {
            return new ArrayList<>(meaningful);
        }

        return tailWithHeader(groupLines, MAX_SNIPPET_LINES);
    }

    /**
     * Returns true if the group lines contain actual content beyond just the
     * {@code ##[group]} header. Used to determine whether to save the current
     * group as a candidate before resetting on the next {@code ##[group]}.
     */
    private boolean hasMeaningfulContent(List<String> lines) {
        return lines.stream().anyMatch(l -> !l.startsWith("##[group]"));
    }

    /**
     * Returns the last {@code maxLines} lines from the list, always preserving
     * the first line (the {@code ##[group]} header) so the snippet always
     * identifies which step failed.
     */
    private List<String> tailWithHeader(List<String> lines, int maxLines) {
        if (lines.size() <= maxLines) {
            return new ArrayList<>(lines);
        }
        List<String> result = new ArrayList<>();
        result.add(lines.get(0));
        result.addAll(lines.subList(lines.size() - (maxLines - 1), lines.size()));
        return result;
    }

    /**
     * Returns true for lines that carry actionable error information worth
     * including in the snippet for known tools. Lines not matching any prefix
     * trigger the fallback line-limit approach via {@link #tailWithHeader}.
     */
    private boolean isMeaningfulLine(String line) {
        return line.startsWith("[ERROR]")
                || line.startsWith("[warn]")
                || line.startsWith("Error:");
    }

    /**
     * Returns true for lines that are GitHub Actions metadata noise —
     * shell declarations, empty lines, and command echoes that add no
     * debugging value to the snippet.
     */
    private boolean isNoiseLine(String line) {
        if (line.isBlank()) {
            return true;
        }
        if (line.startsWith("shell:")) {
            return true;
        }
        if (line.startsWith("[command]")) {
            return true;
        }
        return false;
    }

    /**
     * Strips ANSI terminal escape codes from a log line.
     * GitHub Actions logs contain color codes like {@code [36;1m} and {@code [0m}
     * that are meaningless outside a terminal.
     */
    private String stripAnsiCodes(String line) {
        return line.replaceAll("\\x1B\\[[;\\d]*[A-Za-z]|\\[\\d+m", "");
    }

    /**
     * Strips the leading timestamp from a log line.
     * Lines follow the format {@code 2026-04-01T02:31:11.379Z content}.
     * Returns the original line if no timestamp is found.
     */
    private String stripTimestamp(String line) {
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex > 0) {
            return line.substring(spaceIndex + 1).stripTrailing();
        }
        // Handle lines that are just a timestamp with no content —
        // trailing spaces get stripped in text blocks, leaving a bare timestamp
        if (line.endsWith("Z") && line.contains("T")) {
            return "";
        }
        return line;
    }
}
