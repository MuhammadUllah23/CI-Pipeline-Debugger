CREATE UNIQUE INDEX uq_error_occurrence_cluster_run_step
    ON error_occurrence (error_cluster_id, pipeline_run_id, pipeline_step_id)
    WHERE pipeline_step_id IS NOT NULL;