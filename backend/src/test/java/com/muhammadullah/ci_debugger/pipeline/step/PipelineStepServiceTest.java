package com.muhammadullah.ci_debugger.pipeline.step;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepResponse;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private PipelineStepRepository stepRepository;

    @Mock
    private PipelineRunRepository runRepository;

    @InjectMocks
    private PipelineStepService pipelineStepService;

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

    private PipelineStepSaveRequest buildRequest(String jobName, String stepName, int stepIndex) {
        PipelineStepSaveRequest request = new PipelineStepSaveRequest();
        request.setJobName(jobName);
        request.setStepName(stepName);
        request.setStepIndex(stepIndex);
        request.setStatus(PipelineRunStatus.COMPLETED);
        request.setConclusion(PipelineRunConclusion.SUCCESS);
        request.setStartedAt(Instant.now().minusSeconds(10));
        request.setCompletedAt(Instant.now());
        return request;
    }

    private PipelineStep buildStep(String jobName, String stepName, int stepIndex) {
        PipelineStep step = new PipelineStep(pipelineRun, jobName, stepName, stepIndex);
        step.applyCompletion(
                PipelineRunStatus.COMPLETED,
                PipelineRunConclusion.SUCCESS,
                Instant.now().minusSeconds(10),
                Instant.now(),
                null
        );
        return step;
    }

    @Test
    void saveAll_happyPath_returnsResponses() {
        List<PipelineStepSaveRequest> requests = List.of(
                buildRequest("build", "Checkout code", 1),
                buildRequest("build", "Run tests", 2)
        );

        List<PipelineStep> savedSteps = List.of(
                buildStep("build", "Checkout code", 1),
                buildStep("build", "Run tests", 2)
        );

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(stepRepository.saveAll(anyList())).thenReturn(savedSteps);

        List<PipelineStepResponse> responses = pipelineStepService.saveAll(pipelineRunId, requests);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStepName()).isEqualTo("Checkout code");
        assertThat(responses.get(1).getStepName()).isEqualTo("Run tests");
        verify(stepRepository).saveAll(anyList());
    }

    @Test
    void saveAll_runNotFound_throwsPipelineRunNotFound() {
        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineStepService.saveAll(pipelineRunId, List.of(buildRequest("build", "Checkout code", 1))))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_RUN_NOT_FOUND);
                    assertThat(serviceException.getDetails()).containsKey("pipelineRunId");
                });

        verify(stepRepository, never()).saveAll(anyList());
    }

    @Test
    void saveAll_databaseFailure_throwsDbUpsertFailed() {
        List<PipelineStepSaveRequest> requests = List.of(buildRequest("build", "Checkout code", 1));

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(stepRepository.saveAll(anyList())).thenThrow(new RuntimeException("connection timeout"));

        assertThatThrownBy(() -> pipelineStepService.saveAll(pipelineRunId, requests))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_FAILED);
                    assertThat(serviceException.getDetails()).containsKey("pipelineRunId");
                    assertThat(serviceException.getDetails()).containsKey("cause");
                });
    }

    @Test
    void saveAll_serviceExceptionNotRewrapped() {
        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_CONFLICT)
                .addDetail("pipelineRunId", pipelineRunId);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(stepRepository.saveAll(anyList())).thenThrow(original);

        assertThatThrownBy(() -> pipelineStepService.saveAll(pipelineRunId, List.of(buildRequest("build", "Checkout code", 1))))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_CONFLICT);
                });
    }

    @Test
    void saveAll_emptyList_returnsEmptyListWithoutSaving() {
        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(stepRepository.saveAll(anyList())).thenReturn(List.of());

        List<PipelineStepResponse> responses = pipelineStepService.saveAll(pipelineRunId, List.of());

        assertThat(responses).isEmpty();
        verify(stepRepository).saveAll(anyList());
    }

    // ── getStepsForRun ─────────────────────────────────────────────────────

    @Test
    void getStepsForRun_happyPath_returnsStepsOrderedByJobNameAndStepIndex() {
        when(runRepository.existsById(pipelineRunId)).thenReturn(true);
        when(stepRepository.findByPipelineRunIdOrderByJobNameAscStepIndexAsc(pipelineRunId))
                .thenReturn(List.of(
                        buildStep("build", "Checkout code", 1),
                        buildStep("build", "Run tests", 2),
                        buildStep("deploy", "Upload artifact", 1)
                ));

        List<PipelineStepResponse> result = pipelineStepService.getStepsForRun(pipelineRunId);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getJobName()).isEqualTo("build");
        assertThat(result.get(0).getStepName()).isEqualTo("Checkout code");
        assertThat(result.get(1).getStepName()).isEqualTo("Run tests");
        assertThat(result.get(2).getJobName()).isEqualTo("deploy");
        assertThat(result.get(2).getStepName()).isEqualTo("Upload artifact");
    }

    @Test
    void getStepsForRun_runNotFound_throwsPipelineRunNotFound() {
        when(runRepository.existsById(pipelineRunId)).thenReturn(false);

        assertThatThrownBy(() -> pipelineStepService.getStepsForRun(pipelineRunId))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_RUN_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("pipelineRunId");
                });

        verify(stepRepository, never()).findByPipelineRunIdOrderByJobNameAscStepIndexAsc(any());
    }

    @Test
    void getStepsForRun_noSteps_returnsEmptyList() {
        when(runRepository.existsById(pipelineRunId)).thenReturn(true);
        when(stepRepository.findByPipelineRunIdOrderByJobNameAscStepIndexAsc(pipelineRunId))
                .thenReturn(List.of());

        List<PipelineStepResponse> result = pipelineStepService.getStepsForRun(pipelineRunId);

        assertThat(result).isEmpty();
    }

    @Test
    void getStepsForRun_mapsAllStepFields() {
        Instant startedAt = Instant.now().minusSeconds(10);
        Instant completedAt = Instant.now();

        PipelineStep step = new PipelineStep(pipelineRun, "build", "Run tests", 1);
        step.applyCompletion(
                PipelineRunStatus.COMPLETED,
                PipelineRunConclusion.FAILURE,
                startedAt,
                completedAt,
                null
        );

        when(runRepository.existsById(pipelineRunId)).thenReturn(true);
        when(stepRepository.findByPipelineRunIdOrderByJobNameAscStepIndexAsc(pipelineRunId))
                .thenReturn(List.of(step));

        List<PipelineStepResponse> result = pipelineStepService.getStepsForRun(pipelineRunId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobName()).isEqualTo("build");
        assertThat(result.get(0).getStepName()).isEqualTo("Run tests");
        assertThat(result.get(0).getStepIndex()).isEqualTo(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PipelineRunStatus.COMPLETED);
        assertThat(result.get(0).getConclusion()).isEqualTo(PipelineRunConclusion.FAILURE);
        assertThat(result.get(0).getStartedAt()).isEqualTo(startedAt);
        assertThat(result.get(0).getCompletedAt()).isEqualTo(completedAt);
        assertThat(result.get(0).getDurationMs()).isNotNull();
    }
}