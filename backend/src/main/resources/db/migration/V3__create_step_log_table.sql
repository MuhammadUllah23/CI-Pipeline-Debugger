CREATE TABLE IF NOT EXISTS step_log (
  id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
 
  pipeline_step_id uuid          NOT NULL REFERENCES pipeline_step(id) ON DELETE CASCADE,
 
  log_text         text,
 
  byte_size        bigint,                   
  line_count       int,                      
 
  fetched_at       timestamptz   NOT NULL DEFAULT now(),
  created_at       timestamptz   NOT NULL DEFAULT now()
);
 
CREATE UNIQUE INDEX idx_step_log_step_id ON step_log (pipeline_step_id);
 