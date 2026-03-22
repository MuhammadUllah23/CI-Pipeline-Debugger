package com.muhammadullah.ci_debugger.pipeline.job;

import java.util.UUID;

/**
 * Notifies interested parties of pipeline run progress events triggered
 * by job processing. 
 */
public interface RunProgressNotifier {

    /**
     * Called when a processing job starts executing.
     *
     * @param pipelineRunId the ID of the pipeline run being processed
     * @param jobType       the type of job that started
     */
    void onJobStarted(UUID pipelineRunId, ProcessingJobType jobType);

    /**
     * Called when a processing job completes successfully.
     *
     * @param pipelineRunId the ID of the pipeline run being processed
     * @param jobType       the type of job that completed
     */
    void onJobCompleted(UUID pipelineRunId, ProcessingJobType jobType);

    /**
     * Called when a processing job fails.
     *
     * @param pipelineRunId the ID of the pipeline run being processed
     * @param jobType       the type of job that failed
     * @param error         the error message
     * @param willRetry     whether the job will be retried
     */
    void onJobFailed(UUID pipelineRunId, ProcessingJobType jobType, String error, boolean willRetry);
}