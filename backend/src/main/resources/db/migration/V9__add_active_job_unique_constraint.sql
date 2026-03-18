CREATE UNIQUE INDEX uq_active_job_per_run_and_type
    ON processing_job (pipeline_run_id, job_type)
    WHERE status IN ('PENDING', 'IN_PROGRESS');