package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterResponse;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterWithOccurrencesResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorClusterServiceTest {

    @Mock
    private ErrorClusterRepository clusterRepository;

    @Mock
    private ErrorOccurrenceRepository occurrenceRepository;

    @InjectMocks
    private ErrorClusterService errorClusterService;

    // ** ── helpers ────────────────────────────────────────────────────────────

    private ErrorCluster buildCluster(String owner, String repo, String jobName, String stepName) {
        return new ErrorCluster(
                "fingerprint-" + UUID.randomUUID(),
                owner,
                repo,
                jobName,
                stepName,
                "failure");
    }

    private PipelineRun buildRun() {
        return new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
                "repo",
                "123456789",
                PipelineRunStatus.COMPLETED);
    }

    private ErrorOccurrence buildOccurrence(ErrorCluster cluster, PipelineRun run, PipelineStep step) {
        ErrorOccurrence occurrence = new ErrorOccurrence(cluster, run, step);
        occurrence.setSnippet("[ERROR] something went wrong");
        return occurrence;
    }

    // ** ── listAll ────────────────────────────────────────────────────────────

    @Test
    void listAllHappyPathReturnsClustersSortedByOccurrenceCount() {
        ErrorCluster cluster1 = buildCluster("owner", "repo", "build", "Run tests");
        ErrorCluster cluster2 = buildCluster("owner", "repo", "build", "Compile");

        when(clusterRepository.findAllByOrderByOccurrenceCountDesc(any()))
                .thenReturn(List.of(cluster1, cluster2));

        List<ErrorClusterResponse> result = errorClusterService.listAll(10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getJobName()).isEqualTo("build");
        assertThat(result.get(0).getStepName()).isEqualTo("Run tests");
        assertThat(result.get(1).getStepName()).isEqualTo("Compile");
    }

    @Test
    void listAllEmptyClustersReturnsEmptyList() {
        when(clusterRepository.findAllByOrderByOccurrenceCountDesc(any()))
                .thenReturn(List.of());

        List<ErrorClusterResponse> result = errorClusterService.listAll(10);

        assertThat(result).isEmpty();
    }

    @Test
    void listAllLimitClampedToMaxDoesNotExceed100() {
        when(clusterRepository.findAllByOrderByOccurrenceCountDesc(any()))
                .thenReturn(List.of());

        errorClusterService.listAll(999);

        verify(clusterRepository).findAllByOrderByOccurrenceCountDesc(PageRequest.of(0, 100));
    }

    @Test
    void listAllLimitBelowMaxUsesProvidedLimit() {
        when(clusterRepository.findAllByOrderByOccurrenceCountDesc(any()))
                .thenReturn(List.of());

        errorClusterService.listAll(25);

        verify(clusterRepository).findAllByOrderByOccurrenceCountDesc(PageRequest.of(0, 25));
    }

    @Test
    void listAllLimitExactlyAtMaxUsesMaxLimit() {
        when(clusterRepository.findAllByOrderByOccurrenceCountDesc(any()))
                .thenReturn(List.of());

        errorClusterService.listAll(100);

        verify(clusterRepository).findAllByOrderByOccurrenceCountDesc(PageRequest.of(0, 100));
    }

    // ** ── findById ───────────────────────────────────────────────────────────

    @Test
    void findByIdHappyPathReturnsClusterWithOccurrences() {
        UUID clusterId = UUID.randomUUID();
        ErrorCluster cluster = buildCluster("owner", "repo", "build", "Run tests");
        PipelineRun run = buildRun();
        ErrorOccurrence occurrence = buildOccurrence(cluster, run, null);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(occurrenceRepository.findByErrorClusterIdOrderByCreatedAtDesc(eq(clusterId), any()))
                .thenReturn(new PageImpl<>(List.of(occurrence)));

        ErrorClusterWithOccurrencesResponse result = errorClusterService.findById(clusterId, 0);

        assertThat(result).isNotNull();
        assertThat(result.getCluster().getJobName()).isEqualTo("build");
        assertThat(result.getCluster().getStepName()).isEqualTo("Run tests");
        assertThat(result.getOccurrences().getContent()).hasSize(1);
        assertThat(result.getOccurrences().getContent().get(0).getSnippet()).isEqualTo("[ERROR] something went wrong");
    }

    @Test
    void findByIdNoOccurrencesReturnsClusterWithEmptyOccurrencesList() {
        UUID clusterId = UUID.randomUUID();
        ErrorCluster cluster = buildCluster("owner", "repo", "build", "Run tests");

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(occurrenceRepository.findByErrorClusterIdOrderByCreatedAtDesc(eq(clusterId), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ErrorClusterWithOccurrencesResponse result = errorClusterService.findById(clusterId, 0);

        assertThat(result.getCluster()).isNotNull();
        assertThat(result.getOccurrences().getContent()).isEmpty();
    }

    @Test
    void findByIdClusterNotFoundThrowsErrorClusterNotFound() {
        UUID clusterId = UUID.randomUUID();

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> errorClusterService.findById(clusterId, 0))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.ERROR_CLUSTER_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("id");
                });

        verify(occurrenceRepository, never()).findByErrorClusterIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void findByIdWithMultipleOccurrencesReturnsAllOccurrences() {
        UUID clusterId = UUID.randomUUID();
        ErrorCluster cluster = buildCluster("owner", "repo", "build", "Run tests");
        PipelineRun run1 = buildRun();
        PipelineRun run2 = buildRun();

        ErrorOccurrence occurrence1 = buildOccurrence(cluster, run1, null);
        ErrorOccurrence occurrence2 = buildOccurrence(cluster, run2, null);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(occurrenceRepository.findByErrorClusterIdOrderByCreatedAtDesc(eq(clusterId), any()))
                .thenReturn(new PageImpl<>(List.of(occurrence1, occurrence2)));

        ErrorClusterWithOccurrencesResponse result = errorClusterService.findById(clusterId, 0);

        assertThat(result.getOccurrences().getContent()).hasSize(2);
    }

    // ** ── findByRunId ────────────────────────────────────────────────────────

    @Test
    void findByRunIdHappyPathReturnsClustersForRun() {
        UUID runId = UUID.randomUUID();
        ErrorCluster cluster1 = buildCluster("owner", "repo", "build", "Run tests");
        ErrorCluster cluster2 = buildCluster("owner", "repo", "build", "Compile");

        when(clusterRepository.findByRunId(runId)).thenReturn(List.of(cluster1, cluster2));

        List<ErrorClusterResponse> result = errorClusterService.findByRunId(runId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStepName()).isEqualTo("Run tests");
        assertThat(result.get(1).getStepName()).isEqualTo("Compile");
    }

    @Test
    void findByRunIdNoClustersForRunReturnsEmptyList() {
        UUID runId = UUID.randomUUID();

        when(clusterRepository.findByRunId(runId)).thenReturn(List.of());

        List<ErrorClusterResponse> result = errorClusterService.findByRunId(runId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByRunIdMapsAllClusterFields() {
        UUID runId = UUID.randomUUID();
        ErrorCluster cluster = buildCluster("owner", "repo", "build", "Run tests");
        cluster.setRepresentativeMessage("[ERROR] Build failed");

        when(clusterRepository.findByRunId(runId)).thenReturn(List.of(cluster));

        List<ErrorClusterResponse> result = errorClusterService.findByRunId(runId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwner()).isEqualTo("owner");
        assertThat(result.get(0).getRepo()).isEqualTo("repo");
        assertThat(result.get(0).getJobName()).isEqualTo("build");
        assertThat(result.get(0).getStepName()).isEqualTo("Run tests");
        assertThat(result.get(0).getConclusion()).isEqualTo("failure");
        assertThat(result.get(0).getRepresentativeMessage()).isEqualTo("[ERROR] Build failed");
    }
}
