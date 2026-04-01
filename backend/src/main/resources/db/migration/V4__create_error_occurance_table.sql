CREATE TABLE IF NOT EXISTS error_occurrence (
  id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),

  error_cluster_id uuid          NOT NULL REFERENCES error_cluster(id) ON DELETE CASCADE,
  pipeline_run_id  uuid          NOT NULL REFERENCES pipeline_run(id) ON DELETE CASCADE,
  pipeline_step_id uuid          REFERENCES pipeline_step(id) ON DELETE SET NULL,

  line_number      int,
  snippet          text,

  created_at       timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX idx_error_occurrence_cluster_id ON error_occurrence (error_cluster_id);
CREATE INDEX idx_error_occurrence_run_id     ON error_occurrence (pipeline_run_id);
CREATE INDEX idx_error_occurrence_step_id    ON error_occurrence (pipeline_step_id);

CREATE UNIQUE INDEX uq_error_occurrence_cluster_run_step
    ON error_occurrence (error_cluster_id, pipeline_run_id, pipeline_step_id)
    WHERE pipeline_step_id IS NOT NULL;