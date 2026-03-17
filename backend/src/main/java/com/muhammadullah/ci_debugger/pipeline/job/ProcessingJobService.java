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

    @Transactional
    public ProcessingJobResponse enqueue(UUID pipelineRunId, ProcessingJobType jobType) {
        PipelineRun pipelineRun = runRepository.findById(pipelineRunId)
                .orElseThrow(() -> {
                    log.warn("Cannot enqueue {} job — pipeline run {} not found", jobType, pipelineRunId);
                    return ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                            .addDetail("pipelineRunId", pipelineRunId);
                });

        try {
            ProcessingJob job = new ProcessingJob(pipelineRun, jobType);
            ProcessingJobResponse processingJobResponse = ProcessingJobResponse.from(jobRepository.save(job));
            
            log.info("Enqueued {} job {} for pipeline run {}", jobType, processingJobResponse.getId(), pipelineRunId);

            return processingJobResponse;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                    .addDetail("pipelineRunId", pipelineRunId)
                    .addDetail("jobType", jobType)
                    .addDetail("cause", e.getMessage());
        }
    }
}