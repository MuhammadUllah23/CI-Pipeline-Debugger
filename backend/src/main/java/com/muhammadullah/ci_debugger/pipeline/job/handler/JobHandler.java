package com.muhammadullah.ci_debugger.pipeline.job.handler;

import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJob;
import com.muhammadullah.ci_debugger.pipeline.job.ProcessingJobType;

/**
 * Contract for processing a specific type of {@link ProcessingJob}.
 *
 * <p>Each implementation handles exactly one {@link ProcessingJobType}.
 * The scheduler discovers all handlers at startup via Spring's dependency
 * injection and dispatches jobs to the correct handler based on
 * {@link #getJobType()}.
 */
public interface JobHandler {

    /**
     * Returns the job type this handler is responsible for.
     *
     * @return the handled {@link ProcessingJobType}
     */
    ProcessingJobType getJobType();

    /**
     * Executes the job.
     *
     * @param job the job to process
     */
    void handle(ProcessingJob job);
}