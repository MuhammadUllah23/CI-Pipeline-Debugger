package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestRepository;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequestState;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client.GitHubPullRequestApiClient;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client.GitHubPullRequestApiResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubFetchPrDetailsJobHandlerTest {

    @Mock
    private GitHubPullRequestApiClient gitHubPullRequestApiClient;

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private PipelineRunRepository pipelineRunRepository;

    @InjectMocks
    private GitHubFetchPrDetailsJobHandler handler;

    private PipelineRun pipelineRun;
    private ProcessingJob job;

    @BeforeEach
    void setUp() {
        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
                "repo",
                "123456789",
                PipelineRunStatus.COMPLETED);
        pipelineRun.setPrNumber(42);
        job = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_PR_DETAILS);
    }

    @Test
    void getJobTypeReturnsGitHubFetchPrDetails() {
        assertThat(handler.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_PR_DETAILS);
    }

    @Test
    void handlePrDoesNotExistFetchesAndPersists() {
        GitHubPullRequestApiResponse response = buildApiResponse("open", null);
        PullRequest savedPr = buildPullRequest(PullRequestState.OPEN);

        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.empty());
        when(gitHubPullRequestApiClient.fetchPullRequest("owner", "repo", 42))
                .thenReturn(response);
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(savedPr);
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenReturn(pipelineRun);

        handler.handle(job);

        verify(gitHubPullRequestApiClient).fetchPullRequest("owner", "repo", 42);
        verify(pullRequestRepository).save(any(PullRequest.class));
        verify(pipelineRunRepository).save(pipelineRun);
        assertThat(pipelineRun.getPullRequest()).isEqualTo(savedPr);
    }

    @Test
    void handlePrAlreadyExistsSkipsApiCallAndLinks() {
        PullRequest existingPr = buildPullRequest(PullRequestState.OPEN);

        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.of(existingPr));
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenReturn(pipelineRun);

        handler.handle(job);

        verify(gitHubPullRequestApiClient, never()).fetchPullRequest(any(), any(), any(Integer.class));
        verify(pullRequestRepository, never()).save(any(PullRequest.class));
        verify(pipelineRunRepository).save(pipelineRun);
        assertThat(pipelineRun.getPullRequest()).isEqualTo(existingPr);
    }

    @Test
    void handleMergedPrResolvesMergedState() {
        GitHubPullRequestApiResponse response = buildApiResponse("closed", "2024-01-01T00:00:00Z");
        PullRequest savedPr = buildPullRequest(PullRequestState.MERGED);

        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.empty());
        when(gitHubPullRequestApiClient.fetchPullRequest("owner", "repo", 42))
                .thenReturn(response);
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(savedPr);
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenReturn(pipelineRun);

        handler.handle(job);

        verify(pullRequestRepository).save(any(PullRequest.class));
        assertThat(pipelineRun.getPullRequest().getPrState()).isEqualTo(PullRequestState.MERGED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GitHubPullRequestApiResponse buildApiResponse(String state, String mergedAt) {
        GitHubPullRequestApiResponse response = new GitHubPullRequestApiResponse();
        response.setNumber(42);
        response.setTitle("Add feature");
        response.setState(state);
        if (mergedAt != null) {
            response.setMergedAt(java.time.Instant.parse(mergedAt));
        }
        GitHubPullRequestApiResponse.Head head = new GitHubPullRequestApiResponse.Head();
        head.setSha("abc123");
        head.setRef("feature-branch");
        response.setHead(head);
        return response;
    }

    private PullRequest buildPullRequest(PullRequestState state) {
        PullRequest pr = new PullRequest("GITHUB", "owner", "repo", 42);
        pr.applyDetails("Add feature", "abc123", "feature-branch", "open", state);
        return pr;
    }
}
