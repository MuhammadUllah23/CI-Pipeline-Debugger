package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.error.ErrorIngestionService;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.run.github.client.GitHubLogsApiClient;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubFetchLogsAndClusterJobHandlerTest {

    @Mock private GitHubLogsApiClient gitHubLogsApiClient;
    @Mock private PipelineStepService pipelineStepService;
    @Mock private ErrorIngestionService errorIngestionService;

    @InjectMocks
    private GitHubFetchLogsAndClusterJobHandler handler;

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
        job = new ProcessingJob(pipelineRun, ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER);
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

    @Test
    void getJobType_returnsGitHubFetchLogsAndCluster() {
        assertThat(handler.getJobType()).isEqualTo(ProcessingJobType.GITHUB_FETCH_LOGS_AND_CLUSTER);
    }

    @Test
    void handle_happyPath_fetchesLogsBuildsSnippetsAndIngests() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");

        when(pipelineStepService.findFailedSteps(pipelineRun.getId()))
                .thenReturn(List.of(failedStep));
        when(gitHubLogsApiClient.fetchErrorLines(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(Map.of("build", List.of("[ERROR] No POM in this directory")));

        handler.handle(job);

        verify(gitHubLogsApiClient).fetchErrorLines(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId());
        verify(errorIngestionService).ingestErrors(eq(pipelineRun.getId()), any());
    }

    @Test
    void handle_noFailedSteps_skipsLogFetchAndIngestion() {
        when(pipelineStepService.findFailedSteps(pipelineRun.getId()))
                .thenReturn(List.of());

        handler.handle(job);

        verify(gitHubLogsApiClient, never()).fetchErrorLines(any(), any(), any());
        verify(errorIngestionService, never()).ingestErrors(any(), any());
    }

    @Test
    void handle_noMatchingErrorLines_ingestsWithNullSnippet() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");

        when(pipelineStepService.findFailedSteps(pipelineRun.getId()))
                .thenReturn(List.of(failedStep));
        when(gitHubLogsApiClient.fetchErrorLines(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(Map.of());

        handler.handle(job);

        verify(errorIngestionService).ingestErrors(eq(pipelineRun.getId()), any());
    }

    @Test
    void handle_logClientThrowsServiceException_propagatesWithoutWrapping() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ServiceException original = ServiceException.of(ErrorCode.PROVIDER_API_UNAVAILABLE)
                .addDetail("runId", pipelineRun.getProviderRunId());

        when(pipelineStepService.findFailedSteps(pipelineRun.getId()))
                .thenReturn(List.of(failedStep));
        when(gitHubLogsApiClient.fetchErrorLines(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenThrow(original);

        assertThatThrownBy(() -> handler.handle(job))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PROVIDER_API_UNAVAILABLE));

        verify(errorIngestionService, never()).ingestErrors(any(), any());
    }

    @Test
    void handle_ingestionServiceThrowsServiceException_propagatesWithoutWrapping() {
        PipelineStep failedStep = buildFailedStep("build", "Run tests");
        ServiceException original = ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                .addDetail("pipelineRunId", pipelineRun.getId());

        when(pipelineStepService.findFailedSteps(pipelineRun.getId()))
                .thenReturn(List.of(failedStep));
        when(gitHubLogsApiClient.fetchErrorLines(
                pipelineRun.getOwner(), pipelineRun.getRepo(), pipelineRun.getProviderRunId()))
                .thenReturn(Map.of("build", List.of("[ERROR] Build failed")));
        doThrow(original).when(errorIngestionService).ingestErrors(any(), any());

        assertThatThrownBy(() -> handler.handle(job))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DB_UPSERT_FAILED));
    }
}