package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.pipeline.job.handler.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProcessingJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessingJobScheduler.class);
    private static final long BASE_BACKOFF_MS = 30_000L;

    private final ProcessingJobRepository jobRepository;
    private final RunProgressNotifier progressNotifier;
    private final Map<ProcessingJobType, JobHandler> handlers;

    public ProcessingJobScheduler(
            ProcessingJobRepository jobRepository,
            RunProgressNotifier progressNotifier,
            List<JobHandler> jobHandlers
    ) {
        this.jobRepository = jobRepository;
        this.progressNotifier = progressNotifier;
        this.handlers = jobHandlers.stream()
                .collect(Collectors.toMap(JobHandler::getJobType, handler -> handler));
    }

    @Scheduled(fixedDelayString = "${pipeline.job.scheduler.interval-ms}")
    public void processPendingJobs() {
        List<ProcessingJob> eligibleJobs = jobRepository.findEligibleJobs(Instant.now());

        if (eligibleJobs.isEmpty()) {
            return;
        }

        log.info("Processing {} eligible job(s)", eligibleJobs.size());

        for (ProcessingJob job : eligibleJobs) {
            processJob(job);
        }
    }

    private void processJob(ProcessingJob job) {
        log.info("Starting {} job {} for pipeline run {}",
                job.getJobType(), job.getId(), job.getPipelineRun().getId());

        job.markInProgress();
        jobRepository.save(job);
        progressNotifier.onJobStarted(job.getPipelineRun().getId(), job.getJobType());

        try {
            JobHandler handler = handlers.get(job.getJobType());

            if (handler == null) {
                handleUnknownJobType(job);
                return;
            }

            handler.handle(job);

            job.markCompleted();
            jobRepository.save(job);
            progressNotifier.onJobCompleted(job.getPipelineRun().getId(), job.getJobType());
            log.info("Completed {} job {} for pipeline run {}",
                    job.getJobType(), job.getId(), job.getPipelineRun().getId());

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    private void handleUnknownJobType(ProcessingJob job) {
        log.error("No handler registered for job type {} — failing immediately without retry",
                job.getJobType());
        job.markFailedImmediately("No handler registered for job type: " + job.getJobType());
        jobRepository.save(job);
        progressNotifier.onJobFailed(job.getPipelineRun().getId(), job.getJobType(),
                "No handler registered", false);
    }

    private void handleFailure(ProcessingJob job, Exception e) {
        String error = e.getMessage();
        boolean willRetry = job.getAttempts() + 1 < job.getMaxAttempts();
        Instant nextRetryAt = computeNextRetryAt(job.getAttempts());

        log.warn("Failed {} job {} for pipeline run {} — attempt {}/{} willRetry={} error={}",
                job.getJobType(), job.getId(), job.getPipelineRun().getId(),
                job.getAttempts() + 1, job.getMaxAttempts(), willRetry, error);

        job.markFailed(error, nextRetryAt);
        jobRepository.save(job);
        progressNotifier.onJobFailed(job.getPipelineRun().getId(), job.getJobType(), error, willRetry);
    }

    private Instant computeNextRetryAt(int currentAttempts) {
        long backoffMs = BASE_BACKOFF_MS * (long) Math.pow(3, currentAttempts);
        return Instant.now().plusMillis(backoffMs);
    }
}