ALTER TABLE pipeline_run
  ADD COLUMN pr_id uuid REFERENCES pull_request(id) ON DELETE SET NULL;

CREATE INDEX idx_pipeline_run_pr_id ON pipeline_run (pr_id)
  WHERE pr_id IS NOT NULL;