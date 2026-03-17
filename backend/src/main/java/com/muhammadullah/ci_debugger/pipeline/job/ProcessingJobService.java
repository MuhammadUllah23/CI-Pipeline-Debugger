package com.muhammadullah.ci_debugger.pipeline.job;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
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
    public ProcessingJob enqueue(UUID pipelineRunId, ProcessingJobType jobType) {

    }
}