package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.job.dto.ProcessingJobResponse;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessingJobService {

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
        PipelineRun run = runRepository.findById(pipelineRunId)
                .orElseThrow(() -> ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                        .addDetail("pipelineRunId", pipelineRunId));

        try {
            ProcessingJob job = new ProcessingJob(run, jobType);
            ProcessingJobResponse processingJobResponse = ProcessingJobResponse.from(jobRepository.save(job));
            
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