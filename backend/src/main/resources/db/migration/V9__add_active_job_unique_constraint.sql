ALTER TABLE processing_job
    ADD CONSTRAINT uq_active_job_per_run_and_type
    UNIQUE (pipeline_run_id, job_type)
    WHERE status IN ('PENDING', 'IN_PROGRESS');