package com.muhammadullah.ci_debugger.pipeline.job;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * No-op implementation of {@link RunProgressNotifier} for MVP.
 * All methods are intentionally empty — replace with a real implementation
 * when real-time dashboard updates are needed.
 */
@Component
public class NoOpRunProgressNotifier implements RunProgressNotifier {

    @Override
    public void onJobStarted(UUID pipelineRunId, ProcessingJobType jobType) {}

    @Override
    public void onJobCompleted(UUID pipelineRunId, ProcessingJobType jobType) {}

    @Override
    public void onJobFailed(UUID pipelineRunId, ProcessingJobType jobType, String error, boolean willRetry) {}
}