package com.muhammadullah.ci_debugger.pipeline.step;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineStepRepository extends JpaRepository<PipelineStep, UUID> {

    /**
     * Returns all steps for a given pipeline run ordered by job name and step
     * index — the natural reading order when displaying a run's job/step tree
     * in the dashboard.
     *
     * @param pipelineRunId the ID of the pipeline run to fetch steps for
     * @return all steps for the run, ordered by job name then step index
     */
    List<PipelineStep> findByPipelineRunIdOrderByJobNameAscStepIndexAsc(UUID pipelineRunId);

}