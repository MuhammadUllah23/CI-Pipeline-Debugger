package com.muhammadullah.ci_debugger.pipeline.run;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.dto.CommitRunSetResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoStatusResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoSummaryResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RunSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineRunServiceTest {

    @Mock
    private PipelineRunRepository repository;

    @InjectMocks
    private PipelineRunService pipelineRunService;

    // ── helpers ────────────────────────────────────────────────────────────

    private PipelineRun buildRun(String owner, String repo, String workflowName) {
        PipelineRun run = new PipelineRun(
                PipelineRunProvider.GITHUB,
                owner,
                repo,
                "123456789",
                PipelineRunStatus.COMPLETED);
        run.setWorkflowName(workflowName);
        run.setBranch("main");
        run.setHeadSha("abc123");
        return run;
    }

    private PipelineRunUpsertRequest buildRequest(String status) {
        PipelineRunUpsertRequest req = new PipelineRunUpsertRequest();
        req.setProvider("GITHUB");
        req.setOwner("owner");
        req.setRepo("repo");
        req.setProviderRunId("123456789");
        req.setStatus(status);
        req.setWorkflowName("CI");
        req.setBranch("main");
        req.setHeadSha("abc123");
        req.setStartedAt(Instant.now().minusSeconds(60));
        return req;
    }

    // ── upsert — create new ────────────────────────────────────────────────

    @Test
    void upsertNewRunCreatesAndReturnsResponse() {
        PipelineRunUpsertRequest req = buildRequest("queued");
        PipelineRun saved = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(
                PipelineRunProvider.GITHUB, "owner", "repo", "123456789")).thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response).isNotNull();
        assertThat(response.getOwner()).isEqualTo("owner");
        assertThat(response.getRepo()).isEqualTo("repo");
        verify(repository).save(any(PipelineRun.class));
    }

    @Test
    void upsertNewRunAppliesMetadataOnCreate() {
        PipelineRunUpsertRequest req = buildRequest("queued");
        PipelineRun saved = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response.getWorkflowName()).isEqualTo("CI");
        assertThat(response.getBranch()).isEqualTo("main");
        assertThat(response.getHeadSha()).isEqualTo("abc123");
    }

    @Test
    void upsertNewRunWithCompletedAtAppliesCompletion() {
        PipelineRunUpsertRequest req = buildRequest("completed");
        req.setConclusion("failure");
        req.setCompletedAt(Instant.now());

        PipelineRun saved = buildRun("owner", "repo", "CI");
        saved.markCompleted(PipelineRunConclusion.FAILURE, Instant.now(), 5000L);

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response.getConclusion()).isEqualTo(PipelineRunConclusion.FAILURE);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    // ── upsert — update existing ───────────────────────────────────────────

    @Test
    void upsertExistingRunUpdatesStatusAndReturnsResponse() {
        PipelineRunUpsertRequest req = buildRequest("in_progress");
        PipelineRun existing = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(PipelineRun.class))).thenReturn(existing);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response.getStatus()).isEqualTo(PipelineRunStatus.IN_PROGRESS);
        verify(repository).save(existing);
    }

    @Test
    void upsertExistingRunDoesNotOverwriteWorkflowName() {
        PipelineRunUpsertRequest req = buildRequest("in_progress");
        req.setWorkflowName("New Name");

        PipelineRun existing = buildRun("owner", "repo", "CI");
        // workflowName already set on existing — should not be overwritten

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(PipelineRun.class))).thenReturn(existing);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response.getWorkflowName()).isEqualTo("CI");
    }

    @Test
    void upsertExistingRunDoesNotOverwriteStartedAt() {
        PipelineRunUpsertRequest req = buildRequest("in_progress");
        Instant originalStartedAt = Instant.now().minusSeconds(120);
        req.setStartedAt(Instant.now());

        PipelineRun existing = buildRun("owner", "repo", "CI");
        existing.markStarted(originalStartedAt);

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(PipelineRun.class))).thenReturn(existing);

        pipelineRunService.upsert(req);

        assertThat(existing.getStartedAt()).isEqualTo(originalStartedAt);
    }

    @Test
    void upsertExistingRunWithCompletedAtAppliesCompletion() {
        PipelineRunUpsertRequest req = buildRequest("completed");
        req.setConclusion("success");
        req.setCompletedAt(Instant.now());

        PipelineRun existing = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(PipelineRun.class))).thenReturn(existing);

        pipelineRunService.upsert(req);

        assertThat(existing.getConclusion()).isEqualTo(PipelineRunConclusion.SUCCESS);
        assertThat(existing.getCompletedAt()).isNotNull();
    }

    // ── upsert — provider handling ─────────────────────────────────────────

    @Test
    void upsertUnsupportedProviderThrowsProviderNotSupported() {
        PipelineRunUpsertRequest req = buildRequest("queued");
        req.setProvider("UNKNOWN_PROVIDER");

        assertThatThrownBy(() -> pipelineRunService.upsert(req))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_NOT_SUPPORTED);
                    assertThat(se.getDetails()).containsKey("provider");
                });

        verify(repository, never()).save(any());
    }

    @Test
    void upsertProviderIsCaseInsensitiveNormalizedCorrectly() {
        PipelineRunUpsertRequest req = buildRequest("queued");
        req.setProvider("github");

        PipelineRun saved = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response.getProvider()).isEqualTo(PipelineRunProvider.GITHUB);
    }

    // ── upsert — status coercion ───────────────────────────────────────────

    @Test
    void upsertUnknownStatusCoercedToUnknown() {
        PipelineRunUpsertRequest req = buildRequest("some_unknown_status");
        PipelineRun saved = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        // should not throw — unknown status coerced gracefully
        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response).isNotNull();
    }

    @Test
    void upsertNullStatusCoercedToUnknown() {
        PipelineRunUpsertRequest req = buildRequest(null);
        // @NotBlank on the DTO would catch this in production, but the service
        // should still handle it gracefully if called directly
        PipelineRun saved = buildRun("owner", "repo", "CI");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenReturn(saved);

        PipelineRunResponse response = pipelineRunService.upsert(req);

        assertThat(response).isNotNull();
    }

    // ── upsert — error handling ────────────────────────────────────────────

    @Test
    void upsertDatabaseFailureThrowsDbUpsertFailed() {
        PipelineRunUpsertRequest req = buildRequest("queued");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenThrow(new RuntimeException("connection timeout"));

        assertThatThrownBy(() -> pipelineRunService.upsert(req))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_FAILED);
                    assertThat(se.getDetails()).containsKey("cause");
                });
    }

    @Test
    void upsertServiceExceptionNotRewrapped() {
        PipelineRunUpsertRequest req = buildRequest("queued");
        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_CONFLICT)
                .addDetail("providerRunId", "123456789");

        when(repository.findByProviderAndOwnerAndRepoAndProviderRunId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PipelineRun.class))).thenThrow(original);

        assertThatThrownBy(() -> pipelineRunService.upsert(req))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_CONFLICT);
                });
    }

    // ── listGrouped ────────────────────────────────────────────────────────

    @Test
    void listGroupedEmptyDatabaseReturnsEmptyList() {
        when(repository.findRecentRunsPerWorkflow()).thenReturn(List.of());

        List<RepoSummaryResponse> result = pipelineRunService.listGrouped();

        assertThat(result).isEmpty();
    }

    @Test
    void listGroupedSingleRepoSingleWorkflowReturnsOneRepoWithOneWorkflow() {
        List<PipelineRun> runs = List.of(
                buildRun("owner", "repo", "CI"),
                buildRun("owner", "repo", "CI"),
                buildRun("owner", "repo", "CI"));

        when(repository.findRecentRunsPerWorkflow()).thenReturn(runs);

        List<RepoSummaryResponse> result = pipelineRunService.listGrouped();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwner()).isEqualTo("owner");
        assertThat(result.get(0).getRepo()).isEqualTo("repo");
        assertThat(result.get(0).getWorkflows()).hasSize(1);
        assertThat(result.get(0).getWorkflows().get(0).getWorkflowName()).isEqualTo("CI");
        assertThat(result.get(0).getWorkflows().get(0).getRecentRuns()).hasSize(3);
    }

    @Test
    void listGroupedSingleRepoMultipleWorkflowsReturnsOneRepoWithMultipleWorkflows() {
        List<PipelineRun> runs = List.of(
                buildRun("owner", "repo", "CI"),
                buildRun("owner", "repo", "CI"),
                buildRun("owner", "repo", "Deploy"),
                buildRun("owner", "repo", "Deploy"));

        when(repository.findRecentRunsPerWorkflow()).thenReturn(runs);

        List<RepoSummaryResponse> result = pipelineRunService.listGrouped();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkflows()).hasSize(2);
        assertThat(result.get(0).getWorkflows().get(0).getWorkflowName()).isEqualTo("CI");
        assertThat(result.get(0).getWorkflows().get(1).getWorkflowName()).isEqualTo("Deploy");
    }

    @Test
    void listGroupedMultipleReposReturnsOneEntryPerRepo() {
        List<PipelineRun> runs = List.of(
                buildRun("owner", "repo-a", "CI"),
                buildRun("owner", "repo-a", "CI"),
                buildRun("owner", "repo-b", "CI"),
                buildRun("owner", "repo-b", "CI"));

        when(repository.findRecentRunsPerWorkflow()).thenReturn(runs);

        List<RepoSummaryResponse> result = pipelineRunService.listGrouped();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRepo()).isEqualTo("repo-a");
        assertThat(result.get(1).getRepo()).isEqualTo("repo-b");
    }

    @Test
    void listGroupedRunWithNullWorkflowNameGroupedUnderEmptyStringKey() {
        PipelineRun runWithNullWorkflow = buildRun("owner", "repo", null);

        when(repository.findRecentRunsPerWorkflow()).thenReturn(List.of(runWithNullWorkflow));

        List<RepoSummaryResponse> result = pipelineRunService.listGrouped();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkflows()).hasSize(1);
        assertThat(result.get(0).getWorkflows().get(0).getWorkflowName()).isEqualTo("");
    }

    // ── listByRepo ─────────────────────────────────────────────────────────

    @Test
    void listByRepoHappyPathReturnsMappedPage() {
        List<PipelineRun> runs = List.of(
                buildRun("owner", "repo", "CI"),
                buildRun("owner", "repo", "CI"));
        Page<PipelineRun> page = new PageImpl<>(runs, PageRequest.of(0, 20), 2);

        when(repository.findMainBranchRunsByOwnerAndRepo(eq("owner"), eq("repo"), any())).thenReturn(page);

        Page<RunSummaryResponse> result = pipelineRunService.listByRepo("owner", "repo", 0);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getOwner()).isEqualTo("owner");
        assertThat(result.getContent().get(0).getRepo()).isEqualTo("repo");
    }

    @Test
    void listByRepoEmptyRepoReturnsEmptyPage() {
        Page<PipelineRun> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(repository.findMainBranchRunsByOwnerAndRepo(eq("owner"), eq("repo"), any())).thenReturn(emptyPage);

        Page<RunSummaryResponse> result = pipelineRunService.listByRepo("owner", "repo", 0);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void listByRepoSecondPageReturnsCorrectPageMetadata() {
        List<PipelineRun> runs = List.of(buildRun("owner", "repo", "CI"));
        Page<PipelineRun> page = new PageImpl<>(runs, PageRequest.of(1, 20), 21);

        when(repository.findMainBranchRunsByOwnerAndRepo(eq("owner"), eq("repo"), any())).thenReturn(page);

        Page<RunSummaryResponse> result = pipelineRunService.listByRepo("owner", "repo", 1);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(21);
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    void findByIdHappyPathReturnsResponse() {
        UUID id = UUID.randomUUID();
        PipelineRun run = buildRun("owner", "repo", "CI");

        when(repository.findById(id)).thenReturn(Optional.of(run));

        PipelineRunResponse result = pipelineRunService.findById(id);

        assertThat(result).isNotNull();
        assertThat(result.getOwner()).isEqualTo("owner");
        assertThat(result.getRepo()).isEqualTo("repo");
    }

    @Test
    void findByIdNotFoundThrowsPipelineRunNotFound() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineRunService.findById(id))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_RUN_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("id");
                });
    }

    // ── listRepoStatuses ───────────────────────────────────────────────────

    @Test
    void listRepoStatusesHappyPathReturnsMappedResponses() {
        RepoHealthSummary summary = mock(RepoHealthSummary.class);
        when(summary.getOwner()).thenReturn("owner");
        when(summary.getRepo()).thenReturn("repo");
        when(summary.getOverallConclusion()).thenReturn("SUCCESS");

        when(repository.findRepoHealthSummaries()).thenReturn(List.of(summary));

        List<RepoStatusResponse> result = pipelineRunService.listRepoStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwner()).isEqualTo("owner");
        assertThat(result.get(0).getRepo()).isEqualTo("repo");
        assertThat(result.get(0).getOverallConclusion()).isEqualTo("SUCCESS");
        verify(repository).findRepoHealthSummaries();
    }

    @Test
    void listRepoStatusesEmptyRepositoryReturnsEmptyList() {
        when(repository.findRepoHealthSummaries()).thenReturn(List.of());

        List<RepoStatusResponse> result = pipelineRunService.listRepoStatuses();

        assertThat(result).isEmpty();
        verify(repository).findRepoHealthSummaries();
    }

    @Test
    void listRepoStatusesNullConclusionMapsToNull() {
        RepoHealthSummary summary = mock(RepoHealthSummary.class);
        when(summary.getOwner()).thenReturn("owner");
        when(summary.getRepo()).thenReturn("repo");
        when(summary.getOverallConclusion()).thenReturn(null);

        when(repository.findRepoHealthSummaries()).thenReturn(List.of(summary));

        List<RepoStatusResponse> result = pipelineRunService.listRepoStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOverallConclusion()).isNull();
    }

    @Test
    void listRepoStatusesMultipleReposReturnsAllMapped() {
        RepoHealthSummary summary1 = mock(RepoHealthSummary.class);
        when(summary1.getOwner()).thenReturn("owner");
        when(summary1.getRepo()).thenReturn("repo-a");
        when(summary1.getOverallConclusion()).thenReturn("SUCCESS");

        RepoHealthSummary summary2 = mock(RepoHealthSummary.class);
        when(summary2.getOwner()).thenReturn("owner");
        when(summary2.getRepo()).thenReturn("repo-b");
        when(summary2.getOverallConclusion()).thenReturn("FAILURE");

        when(repository.findRepoHealthSummaries()).thenReturn(List.of(summary1, summary2));

        List<RepoStatusResponse> result = pipelineRunService.listRepoStatuses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRepo()).isEqualTo("repo-a");
        assertThat(result.get(0).getOverallConclusion()).isEqualTo("SUCCESS");
        assertThat(result.get(1).getRepo()).isEqualTo("repo-b");
        assertThat(result.get(1).getOverallConclusion()).isEqualTo("FAILURE");
    }

    // ── listMainBranchRunSets ──────────────────────────────────────────────

    @Test
    void listMainBranchRunSetsHappyPathReturnsMappedSets() {
        PipelineRun run = buildRun("owner", "repo", "CI");
        run.setHeadSha("sha1");
        run.setStartedAt(Instant.now().minusSeconds(60));

        when(repository.findDistinctMainBranchHeadShas("owner", "repo"))
                .thenReturn(List.of("sha1"));
        when(repository.findMainBranchRunsByHeadShaIn(eq("owner"), eq("repo"), any()))
                .thenReturn(List.of(run));

        Page<CommitRunSetResponse> result = pipelineRunService.listMainBranchRunSets("owner", "repo", 0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getHeadSha()).isEqualTo("sha1");
        assertThat(result.getContent().get(0).getRuns()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listMainBranchRunSetsEmptyHeadShasReturnsEmptyPage() {
        when(repository.findDistinctMainBranchHeadShas("owner", "repo"))
                .thenReturn(List.of());

        Page<CommitRunSetResponse> result = pipelineRunService.listMainBranchRunSets("owner", "repo", 0);

        assertThat(result.getContent()).isEmpty();
        verify(repository, never()).findMainBranchRunsByHeadShaIn(any(), any(), any());
    }

    @Test
    void listMainBranchRunSetsPageOutOfBoundsReturnsEmptyPage() {
        when(repository.findDistinctMainBranchHeadShas("owner", "repo"))
                .thenReturn(List.of("sha1"));

        Page<CommitRunSetResponse> result = pipelineRunService.listMainBranchRunSets("owner", "repo", 5);

        assertThat(result.getContent()).isEmpty();
        verify(repository, never()).findMainBranchRunsByHeadShaIn(any(), any(), any());
    }

    @Test
    void listMainBranchRunSetsMultipleShasGroupsRunsCorrectly() {
        PipelineRun run1 = buildRun("owner", "repo", "CI");
        run1.setHeadSha("sha1");
        run1.setStartedAt(Instant.now().minusSeconds(60));

        PipelineRun run2 = buildRun("owner", "repo", "CI");
        run2.setHeadSha("sha2");
        run2.setStartedAt(Instant.now().minusSeconds(120));

        when(repository.findDistinctMainBranchHeadShas("owner", "repo"))
                .thenReturn(List.of("sha1", "sha2"));
        when(repository.findMainBranchRunsByHeadShaIn(eq("owner"), eq("repo"), any()))
                .thenReturn(List.of(run1, run2));

        Page<CommitRunSetResponse> result = pipelineRunService.listMainBranchRunSets("owner", "repo", 0);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getHeadSha()).isEqualTo("sha1");
        assertThat(result.getContent().get(1).getHeadSha()).isEqualTo("sha2");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

}
