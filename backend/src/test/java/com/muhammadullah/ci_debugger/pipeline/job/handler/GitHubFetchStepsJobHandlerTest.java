package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobService;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.job.dto.ProcessingJobResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.run.github.client.GitHubStepsApiClient;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStepService;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubFetchStepsJobHandlerTest {

    @Mock private GitHubStepsApiClient gitHubStepsApiClient;
    @Mock private PipelineStepService pipelineStepService;
    @Mock private ProcessingJobService processingJobService;

    @InjectMocks
    private GitHubFetchStepsJobHandler handler;

    private PipelineRun pipelineRun;
    private ProcessingJob job;

    @BeforeEach
    void setUp() {
        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "mhu-ventures",
                "ci-pipeline-debugger",
                "123456789",
                PipelineRunStatus.COMPLETED
        );
        job = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_STEPS);
    }

    private PipelineStepSaveRequest buildStepRequest(String jobName, String stepName) {
        PipelineStepSaveRequest request = new PipelineStepSaveRequest();
        request.setJobName(jobName);
        request.setStepName(stepName);
        request.setStepIndex(1);
        return request;
    }

    private ProcessingJobResponse buildJobResponse() {
        ProcessingJob logsJob = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER);
        return ProcessingJobResponse.from(logsJob);
    }

    @Test
    void getJobType_returnsGitHubFetchSteps() {
        assertThat(handler.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_STEPS);
    }

    @Test
    void handle_happyPath_fetchesStepsSavesAndEnqueuesLogsJob() {
        pipelineRun.markCompleted(PipelineRunConclusion.FAILURE, java.time.Instant.now(), 1000L);

        List<PipelineStepSaveRequest> stepRequests = List.of(
                buildStepRequest("build", "Checkout code"),
                buildStepRequest("build", "Run tests")
        );

        when(gitHubStepsApiClient.fetchSteps(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(stepRequests);
        when(processingJobService.enqueue(pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER))
                .thenReturn(buildJobResponse());

        handler.handle(job);

        verify(gitHubStepsApiClient).fetchSteps(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId());
        verify(pipelineStepService).saveAll(eq(pipelineRun.getId()), any());
        verify(processingJobService).enqueue(pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER);
    }

    @Test
    void handle_successfulRun_stepsAreSavedButLogsJobNotEnqueued() {
        pipelineRun.markCompleted(PipelineRunConclusion.SUCCESS, java.time.Instant.now(), 1000L);

        List<PipelineStepSaveRequest> stepRequests = List.of(
                buildStepRequest("build", "Checkout code")
        );

        when(gitHubStepsApiClient.fetchSteps(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(stepRequests);

        handler.handle(job);

        verify(pipelineStepService).saveAll(eq(pipelineRun.getId()), any());
        verify(processingJobService, never()).enqueue(any(UUID.class), any());
    }

    @Test
    void handle_stepsClientThrowsServiceException_propagatesWithoutWrapping() {
        ServiceException original = ServiceException.of(ErrorCode.PROVIDER_API_UNAVAILABLE)
                .addDetail("runId", pipelineRun.getProviderRunId());

        when(gitHubStepsApiClient.fetchSteps(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenThrow(original);

        assertThatThrownBy(() -> handler.handle(job))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE));

        verify(pipelineStepService, never()).saveAll(any(), any());
        verify(processingJobService, never()).enqueue(any(), any());
    }

    @Test
    void handle_stepServiceThrowsServiceException_propagatesWithoutWrapping() {
        pipelineRun.markCompleted(PipelineRunConclusion.FAILURE, java.time.Instant.now(), 1000L);

        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                .addDetail("pipelineRunId", pipelineRun.getId());

        when(gitHubStepsApiClient.fetchSteps(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(List.of(buildStepRequest("build", "Run tests")));
        when(pipelineStepService.saveAll(any(), any())).thenThrow(original);

        assertThatThrownBy(() -> handler.handle(job))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DB_UPSERT_FAILED));

        verify(processingJobService, never()).enqueue(any(), any());
    }
}