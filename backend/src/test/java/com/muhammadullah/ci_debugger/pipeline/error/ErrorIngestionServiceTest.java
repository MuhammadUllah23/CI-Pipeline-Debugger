package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorIngestionServiceTest {

    @Mock private ErrorClusterRepository clusterRepository;
    @Mock private ErrorOccurrenceRepository occurrenceRepository;
    @Mock private PipelineRunRepository runRepository;

    @InjectMocks
    private ErrorIngestionService errorIngestionService;

    private UUID pipelineRunId;
    private PipelineRun pipelineRun;

    @BeforeEach
    void setUp() {
        pipelineRunId = UUID.randomUUID();
        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
                "ci-pipeline-debugger",
                "123456789",
                PipelineRunStatus.COMPLETED
        );
    }

    private PipelineStep buildFailedStep(String jobName, String stepName) {
        PipelineStep step = new PipelineStep(pipelineRun, jobName, stepName, 1);
        step.applyCompletion(
                PipelineRunStatus.COMPLETED,
                PipelineRunConclusion.FAILURE,
                Instant.now().minusSeconds(10),
                Instant.now(),
                null
        );
        return step;
    }

    private ErrorCluster buildCluster() {
        return new ErrorCluster(
                "abc123",
                "owner",
                "ci-pipeline-debugger",
                "build",
                "Run tests",
                "FAILURE"
        );
    }

    @Test
    void ingestErrors_happyPath_newClusterCreatedAndOccurrenceSaved() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ErrorCluster savedCluster = buildCluster();
        String snippet = "[ERROR] No POM in this directory\n##[error]Process completed with exit code 1.";

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString())).thenReturn(Optional.empty());
        when(clusterRepository.save(any(ErrorCluster.class))).thenReturn(savedCluster);
        when(occurrenceRepository.existsByErrorClusterAndPipelineRun(savedCluster, pipelineRun))
                .thenReturn(false);

        errorIngestionService.ingestErrors(pipelineRunId, Map.of(failedStep, snippet));

        verify(clusterRepository).save(any(ErrorCluster.class));
        verify(occurrenceRepository).save(any(ErrorOccurrence.class));
    }

    @Test
    void ingestErrors_existingCluster_occurrenceCountIncrementedAndOccurrenceSaved() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ErrorCluster existingCluster = buildCluster();
        long originalCount = existingCluster.getOccurrenceCount();
        String snippet = "[ERROR] No POM in this directory";

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString())).thenReturn(Optional.of(existingCluster));
        when(clusterRepository.save(existingCluster)).thenReturn(existingCluster);
        when(occurrenceRepository.existsByErrorClusterAndPipelineRun(existingCluster, pipelineRun))
                .thenReturn(false);

        errorIngestionService.ingestErrors(pipelineRunId, Map.of(failedStep, snippet));

        assertThat(existingCluster.getOccurrenceCount()).isEqualTo(originalCount + 1);
        verify(occurrenceRepository).save(any(ErrorOccurrence.class));
    }

    @Test
    void ingestErrors_nullSnippet_occurrenceSavedWithNullSnippet() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ErrorCluster savedCluster = buildCluster();
        Map<PipelineStep, String> snippetByStep = new java.util.HashMap<>();
        snippetByStep.put(failedStep, null);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString())).thenReturn(Optional.empty());
        when(clusterRepository.save(any(ErrorCluster.class))).thenReturn(savedCluster);
        when(occurrenceRepository.existsByErrorClusterAndPipelineRun(savedCluster, pipelineRun))
                .thenReturn(false);

        errorIngestionService.ingestErrors(pipelineRunId, snippetByStep);

        verify(occurrenceRepository).save(any(ErrorOccurrence.class));
    }

    @Test
    void ingestErrors_runNotFound_throwsPipelineRunNotFound() {
        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> errorIngestionService.ingestErrors(pipelineRunId, Map.of()))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_RUN_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("pipelineRunId");
                });

        verify(clusterRepository, never()).save(any());
        verify(occurrenceRepository, never()).save(any());
    }

    @Test
    void ingestErrors_occurrenceAlreadyExists_skipsOccurrenceCreation() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ErrorCluster existingCluster = buildCluster();

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString())).thenReturn(Optional.of(existingCluster));
        when(clusterRepository.save(existingCluster)).thenReturn(existingCluster);
        when(occurrenceRepository.existsByErrorClusterAndPipelineRun(existingCluster, pipelineRun))
                .thenReturn(true);

        errorIngestionService.ingestErrors(pipelineRunId, Map.of(failedStep, "[ERROR] Build failed"));

        verify(occurrenceRepository, never()).save(any());
    }

    @Test
    void ingestErrors_databaseFailure_throwsDbUpsertFailed() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString()))
                .thenThrow(new RuntimeException("connection timeout"));

        assertThatThrownBy(() -> errorIngestionService.ingestErrors(
                pipelineRunId, Map.of(failedStep, "[ERROR] Build failed")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_FAILED);
                    assertThat(se.getDetails()).containsKey("pipelineRunId");
                    assertThat(se.getDetails()).containsKey("cause");
                });
    }

    @Test
    void ingestErrors_serviceExceptionNotRewrapped() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_CONFLICT)
                .addDetail("pipelineRunId", pipelineRunId);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(clusterRepository.findByFingerprint(anyString())).thenThrow(original);

        assertThatThrownBy(() -> errorIngestionService.ingestErrors(
                pipelineRunId, Map.of(failedStep, "[ERROR] Build failed")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DB_UPSERT_CONFLICT));
    }
}