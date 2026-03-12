CREATE TABLE IF NOT EXISTS pipeline_step (
  id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
 
  pipeline_run_id  uuid          NOT NULL REFERENCES pipeline_run(id) ON DELETE CASCADE,
 

  job_name         varchar(300),             
  step_name        varchar(300)  NOT NULL,
  step_index       int           NOT NULL,   
 

  status           varchar(30),              
  conclusion       varchar(30),              
 

  started_at       timestamptz,
  completed_at     timestamptz,
  duration_ms      bigint,
 
  created_at       timestamptz   NOT NULL DEFAULT now(),
 
  CONSTRAINT uq_pipeline_step UNIQUE (pipeline_run_id, job_name, step_index)
);
 
CREATE INDEX idx_pipeline_step_run_id    ON pipeline_step (pipeline_run_id);
