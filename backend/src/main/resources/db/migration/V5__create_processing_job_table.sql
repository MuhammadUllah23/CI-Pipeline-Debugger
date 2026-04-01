CREATE TABLE IF NOT EXISTS processing_job (
  id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),

  pipeline_run_id  uuid          NOT NULL REFERENCES pipeline_run(id) ON DELETE CASCADE,

  job_type         varchar(50)   NOT NULL,

  status           varchar(20)   NOT NULL DEFAULT 'PENDING',

  attempts         int           NOT NULL DEFAULT 0,
  max_attempts     int           NOT NULL DEFAULT 3,
  last_error       text,

  -- Timing
  scheduled_at     timestamptz   NOT NULL DEFAULT now(),
  next_retry_at    timestamptz,
  started_at       timestamptz,
  completed_at     timestamptz,

  created_at       timestamptz   NOT NULL DEFAULT now(),
  updated_at       timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX idx_processing_job_status       ON processing_job (status)
  WHERE status IN ('PENDING', 'IN_PROGRESS');

CREATE INDEX idx_processing_job_run_id       ON processing_job (pipeline_run_id);
CREATE INDEX idx_processing_job_scheduled_at ON processing_job (scheduled_at ASC)
  WHERE status = 'PENDING';

CREATE UNIQUE INDEX uq_active_job_per_run_and_type
    ON processing_job (pipeline_run_id, job_type)
    WHERE status IN ('PENDING', 'IN_PROGRESS');