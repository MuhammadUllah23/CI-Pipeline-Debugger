package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.dto.PullRequestResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTest {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private PipelineRunRepository pipelineRunRepository;

    @InjectMocks
    private PullRequestService pullRequestService;

    private PullRequest pullRequest;
    private PipelineRun pipelineRun;

    @BeforeEach
    void setUp() {
        pullRequest = new PullRequest("GITHUB", "owner", "repo", 42);
        pullRequest.setId(UUID.randomUUID());
        pullRequest.applyDetails("Add feature", "abc123", "feature-branch", "open", PullRequestState.OPEN);

        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
                "repo",
                "123456789",
                PipelineRunStatus.COMPLETED);

        pipelineRun.setId(UUID.randomUUID());
        pipelineRun.setPullRequest(pullRequest);
    }

    @Test
    void listOpenWithLatestRunReturnsMappedResponses() {
        when(pipelineRunRepository.findLatestRunPerWorkflowForOpenPullRequests())
                .thenReturn(List.of(pipelineRun));

        List<PullRequestResponse> responses = pullRequestService.listOpenWithLatestRun();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPrNumber()).isEqualTo(42);
        assertThat(responses.get(0).getTitle()).isEqualTo("Add feature");
        assertThat(responses.get(0).getPrState()).isEqualTo(PullRequestState.OPEN);
        assertThat(responses.get(0).getRuns()).hasSize(1);
        verify(pipelineRunRepository).findLatestRunPerWorkflowForOpenPullRequests();
    }

    @Test
    void listOpenWithLatestRunNoOpenPrsReturnsEmptyList() {
        when(pipelineRunRepository.findLatestRunPerWorkflowForOpenPullRequests())
                .thenReturn(List.of());

        List<PullRequestResponse> responses = pullRequestService.listOpenWithLatestRun();

        assertThat(responses).isEmpty();
        verify(pipelineRunRepository).findLatestRunPerWorkflowForOpenPullRequests();
    }

    @Test
    void findByIdHappyPathReturnsPrWithRuns() {
        UUID prId = UUID.randomUUID();
        Page<PipelineRun> page = new PageImpl<>(List.of(pipelineRun));

        when(pullRequestRepository.findById(prId)).thenReturn(Optional.of(pullRequest));
        when(pipelineRunRepository.findByPullRequestIdOrderByCreatedAtDesc(
                eq(prId), any(PageRequest.class)))
                .thenReturn(page);

        PullRequestResponse response = pullRequestService.findById(prId, 0);

        assertThat(response).isNotNull();
        assertThat(response.getPrNumber()).isEqualTo(42);
        assertThat(response.getRuns()).hasSize(1);
        verify(pullRequestRepository).findById(prId);
        verify(pipelineRunRepository).findByPullRequestIdOrderByCreatedAtDesc(
                eq(prId), any(PageRequest.class));
    }

    @Test
    void findByIdNotFoundThrowsDbRecordNotFound() {
        UUID prId = UUID.randomUUID();

        when(pullRequestRepository.findById(prId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestService.findById(prId, 0))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_RECORD_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("pullRequestId");
                });

        verify(pipelineRunRepository, never()).findByPullRequestIdOrderByCreatedAtDesc(
                any(), any());
    }

    @Test
    void findByIdEmptyRunsPageReturnsPrWithEmptyRunsList() {
        UUID prId = UUID.randomUUID();
        Page<PipelineRun> emptyPage = new PageImpl<>(List.of());

        when(pullRequestRepository.findById(prId)).thenReturn(Optional.of(pullRequest));
        when(pipelineRunRepository.findByPullRequestIdOrderByCreatedAtDesc(
                eq(prId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        PullRequestResponse response = pullRequestService.findById(prId, 0);

        assertThat(response).isNotNull();
        assertThat(response.getRuns()).isEmpty();
    }

    @Test
    void updateStateMergedPrUpdatesPrStateToMerged() {
        UUID prId = UUID.randomUUID();
        pullRequest.setId(prId);
        Instant mergedAt = Instant.parse("2024-01-01T00:00:00Z");

        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.of(pullRequest));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(pullRequest);

        pullRequestService.updateState("GITHUB", "owner", "repo", 42, mergedAt);

        assertThat(pullRequest.getPrState()).isEqualTo(PullRequestState.MERGED);
        assertThat(pullRequest.getPrStateRaw()).isEqualTo("merged");
        verify(pullRequestRepository).save(pullRequest);
    }

    @Test
    void updateStateClosedPrUpdatesPrStateToClosed() {
        UUID prId = UUID.randomUUID();
        pullRequest.setId(prId);

        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.of(pullRequest));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(pullRequest);

        pullRequestService.updateState("GITHUB", "owner", "repo", 42, null);

        assertThat(pullRequest.getPrState()).isEqualTo(PullRequestState.CLOSED);
        assertThat(pullRequest.getPrStateRaw()).isEqualTo("closed");
        verify(pullRequestRepository).save(pullRequest);
    }

    @Test
    void updateStatePrNotFoundDoesNothing() {
        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.empty());

        pullRequestService.updateState("GITHUB", "owner", "repo", 42, null);

        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void findOrCreatePrDoesNotExistCreatesNewPr() {
        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.empty());
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(pullRequest);

        PullRequest result = pullRequestService.findOrCreate("GITHUB", "owner", "repo", 42);

        assertThat(result).isNotNull();
        assertThat(result.getPrNumber()).isEqualTo(42);
        verify(pullRequestRepository).save(any(PullRequest.class));
    }

    @Test
    void findOrCreatePrAlreadyExistsReturnsExisting() {
        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.of(pullRequest));

        PullRequest result = pullRequestService.findOrCreate("GITHUB", "owner", "repo", 42);

        assertThat(result).isEqualTo(pullRequest);
        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void findOrCreateConcurrentInsertReturnsExistingPr() {
        when(pullRequestRepository.findByProviderAndOwnerAndRepoAndPrNumber(
                "GITHUB", "owner", "repo", 42))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pullRequest));
        when(pullRequestRepository.save(any(PullRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PullRequest result = pullRequestService.findOrCreate("GITHUB", "owner", "repo", 42);

        assertThat(result).isEqualTo(pullRequest);
        verify(pullRequestRepository, times(2))
                .findByProviderAndOwnerAndRepoAndPrNumber("GITHUB", "owner", "repo", 42);
    }
}
