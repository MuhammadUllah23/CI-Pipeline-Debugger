package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.pipeline.job.handler.JobHandler;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunProvider;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingJobSchedulerTest {

    @Mock
    private ProcessingJobRepository jobRepository;

    @Mock
    private RunProgressNotifier progressNotifier;

    @Mock
    private JobHandler gitHubFetchStepsHandler;

    private ProcessingJobScheduler scheduler;
    private PipelineRun pipelineRun;

    @BeforeEach
    void setUp() {
        when(gitHubFetchStepsHandler.getJobType()).thenReturn(ProcessingJobType.GITHUB_FETCH_STEPS);

        scheduler = new ProcessingJobScheduler(
                jobRepository,
                progressNotifier,
                List.of(gitHubFetchStepsHandler)
        );

        pipelineRun = new PipelineRun(
                PipelineRunProvider.GITHUB,
                "owner",
                "ci-pipeline-debugger",
                "123456789",
                PipelineRunStatus.COMPLETED
        );
    }

    private ProcessingJob buildJob(ProcessingJobType jobType) {
        return new ProcessingJob(pipelineRun, jobType);
    }

    @Test
    void processPendingJobs_noEligibleJobs_doesNothing() {
        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of());

        scheduler.processPendingJobs();

        verify(jobRepository, never()).save(any());
        verify(progressNotifier, never()).onJobStarted(any(), any());
    }

    @Test
    void processPendingJobs_happyPath_jobMarkedCompletedWithOneAttempt() {
        ProcessingJob job = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);

        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of(job));
        when(jobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.COMPLETED);
        assertThat(job.getAttempts()).isEqualTo(1);
        verify(gitHubFetchStepsHandler).handle(job);
        verify(progressNotifier).onJobStarted(pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_STEPS);
        verify(progressNotifier).onJobCompleted(pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_STEPS);
    }

    @Test
    void processPendingJobs_handlerThrowsException_jobMarkedFailedWithRetry() {
        ProcessingJob job = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);

        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of(job));
        when(jobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("GitHub API timeout")).when(gitHubFetchStepsHandler).handle(job);

        scheduler.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.PENDING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getLastError()).isEqualTo("GitHub API timeout");
        assertThat(job.getNextRetryAt()).isNotNull();
        verify(progressNotifier).onJobFailed(
                pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_STEPS, "GitHub API timeout", true);
    }

    @Test
    void processPendingJobs_allAttemptsExhausted_jobMarkedFailed() {
        ProcessingJob job = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);

        // simulate two previous attempts already made
        job.incrementAttempts();
        job.markFailed("previous failure", Instant.now());
        job.incrementAttempts();
        job.markFailed("previous failure", Instant.now());

        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of(job));
        when(jobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("GitHub API timeout")).when(gitHubFetchStepsHandler).handle(job);

        scheduler.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo(3);
        verify(progressNotifier).onJobFailed(
                pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_STEPS, "GitHub API timeout", false);
    }

    @Test
    void processPendingJobs_unknownJobType_jobMarkedFailedImmediately() {
        ProcessingJob job = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);

        ProcessingJobScheduler emptyHandlerScheduler = new ProcessingJobScheduler(
                jobRepository,
                progressNotifier,
                List.of()
        );

        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of(job));
        when(jobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emptyHandlerScheduler.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo(job.getMaxAttempts());
        assertThat(job.getLastError()).contains("No handler registered");
        verify(progressNotifier).onJobFailed(
                pipelineRun.getId(), ProcessingJobType.GITHUB_FETCH_STEPS,
                "No handler registered for job type: GITHUB_FETCH_STEPS", false);
    }

    @Test
    void processPendingJobs_multipleEligibleJobs_allProcessed() {
        ProcessingJob job1 = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);
        ProcessingJob job2 = buildJob(ProcessingJobType.GITHUB_FETCH_STEPS);

        when(jobRepository.findEligibleJobs(any(Instant.class))).thenReturn(List.of(job1, job2));
        when(jobRepository.save(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.processPendingJobs();

        assertThat(job1.getStatus()).isEqualTo(ProcessingJobStatus.COMPLETED);
        assertThat(job1.getAttempts()).isEqualTo(1);
        assertThat(job2.getStatus()).isEqualTo(ProcessingJobStatus.COMPLETED);
        assertThat(job2.getAttempts()).isEqualTo(1);
        verify(gitHubFetchStepsHandler).handle(job1);
        verify(gitHubFetchStepsHandler).handle(job2);
    }
}