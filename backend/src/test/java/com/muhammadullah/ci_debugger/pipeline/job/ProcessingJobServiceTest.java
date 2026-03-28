package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.job.dto.ProcessingJobResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingJobServiceTest {

    @Mock
    private ProcessingJobRepository jobRepository;

    @Mock
    private PipelineRunRepository runRepository;

    @InjectMocks
    private ProcessingJobService processingJobService;

    private UUID pipelineRunId;
    private PipelineRun pipelineRun;

    @BeforeEach
    void setUp() {
        pipelineRunId = UUID.randomUUID();
        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "mhu-ventures",
                "ci-pipeline-debugger",
                "123456789",
                PipelineRunStatus.COMPLETED
        );
    }

    @Test
    void enqueue_happyPath_returnsResponse() {
        ProcessingJob savedJob = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_STEPS);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(jobRepository.save(any(ProcessingJob.class))).thenReturn(savedJob);

        ProcessingJobResponse response = processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS);

        assertThat(response).isNotNull();
        assertThat(response.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_STEPS);
        assertThat(response.getStatus()).isEqualTo(ProcessingJobStatus.PENDING);
        verify(jobRepository).save(any(ProcessingJob.class));
    }

    @Test
    void enqueue_runNotFound_throwsPipelineRunNotFound() {
        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.PIPELINE_RUN_NOT_FOUND);
                    assertThat(se.getDetails()).containsKey("pipelineRunId");
                });

        verify(jobRepository, never()).save(any());
    }

    @Test
    void enqueue_databaseFailure_throwsDbUpsertFailed() {
        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(jobRepository.save(any(ProcessingJob.class))).thenThrow(new RuntimeException("connection timeout"));

        assertThatThrownBy(() -> processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_FAILED);
                    assertThat(se.getDetails()).containsKey("pipelineRunId");
                    assertThat(se.getDetails()).containsKey("jobType");
                    assertThat(se.getDetails()).containsKey("cause");
                });
    }

    @Test
    void enqueue_serviceExceptionNotRewrapped() {
        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_CONFLICT)
                .addDetail("pipelineRunId", pipelineRunId);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(jobRepository.save(any(ProcessingJob.class))).thenThrow(original);

        assertThatThrownBy(() -> processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.DB_UPSERT_CONFLICT);
                });
    }

    @Test
    void enqueue_activeJobAlreadyExists_returnsExistingJobWithoutSaving() {
        ProcessingJob existingJob = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_STEPS);

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(jobRepository.findNonFailedJobByRunIdAndType(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS))
                .thenReturn(Optional.of(existingJob));

        ProcessingJobResponse response = processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS);

        assertThat(response).isNotNull();
        assertThat(response.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_STEPS);
        assertThat(response.getStatus()).isEqualTo(ProcessingJobStatus.PENDING);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void enqueue_completedJobAlreadyExists_returnsExistingJobWithoutSaving() {
        ProcessingJob completedJob = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_STEPS);
        completedJob.markInProgress();
        completedJob.markCompleted();

        when(runRepository.findById(pipelineRunId)).thenReturn(Optional.of(pipelineRun));
        when(jobRepository.findNonFailedJobByRunIdAndType(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS))
                .thenReturn(Optional.of(completedJob));

        ProcessingJobResponse response = processingJobService.enqueue(pipelineRunId, ProcessingJobType.GITHUB_FETCH_STEPS);

        assertThat(response).isNotNull();
        assertThat(response.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_STEPS);
        assertThat(response.getStatus()).isEqualTo(ProcessingJobStatus.COMPLETED);
        verify(jobRepository, never()).save(any());
    }
}