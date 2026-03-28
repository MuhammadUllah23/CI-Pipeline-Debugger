package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.job.dto.ProcessingJobResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessingJobService {
    private static final Logger log = LoggerFactory.getLogger(ProcessingJobService.class);


    private final ProcessingJobRepository jobRepository;
    private final PipelineRunRepository runRepository;

    public ProcessingJobService(
            ProcessingJobRepository jobRepository,
            PipelineRunRepository runRepository
    ) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
    }

    /**
     * Creates a new {@link ProcessingJob} of the given type for the specified pipeline run.
     *
     * <p>The job is created with {@link ProcessingJobStatus#PENDING} and is immediately
     * eligible for pickup by the scheduler.
     *
     * @param pipelineRunId the ID of the pipeline run to enqueue the job for
     * @param jobType       the type of job to enqueue
     * @return the created job
     * @throws ServiceException with {@link ErrorCode#PIPELINE_RUN_NOT_FOUND} if no
     *                          run exists for the given ID
     * @throws ServiceException with {@link ErrorCode#DB_UPSERT_FAILED} if an
     *                          unexpected database error occurs
     */
@Transactional
public ProcessingJobResponse enqueue(UUID pipelineRunId, ProcessingJobType jobType) {
    PipelineRun run = runRepository.findById(pipelineRunId)
            .orElseThrow(() -> {
                log.warn("Cannot enqueue {} job — pipeline run {} not found", jobType, pipelineRunId);
                return ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                        .addDetail("pipelineRunId", pipelineRunId);
            });

    Optional<ProcessingJob> existingJob = jobRepository.findNonFailedJobByRunIdAndType(pipelineRunId, jobType);
    if (existingJob.isPresent()) {
        log.info("Active {} job already exists for pipeline run {} — skipping duplicate enqueue",
                jobType, pipelineRunId);
        return ProcessingJobResponse.from(existingJob.get());
    }

    try {
        ProcessingJob job = new ProcessingJob(run, jobType);
        ProcessingJobResponse response = ProcessingJobResponse.from(jobRepository.save(job));
        log.info("Enqueued {} job {} for pipeline run {}", jobType, response.getId(), pipelineRunId);
        return response;
    } catch (ServiceException e) {
        throw e;
    } catch (Exception e) {
        log.error("Failed to enqueue {} job for pipeline run {} — {}", jobType, pipelineRunId, e.getMessage());
        throw ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                .addDetail("pipelineRunId", pipelineRunId)
                .addDetail("jobType", jobType)
                .addDetail("cause", e.getMessage());
    }
}
}