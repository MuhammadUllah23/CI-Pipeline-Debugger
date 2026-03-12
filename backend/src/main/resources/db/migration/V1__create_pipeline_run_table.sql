CREATE EXTENSION IF NOT EXISTS "pgcrypto";
 
CREATE TABLE IF NOT EXISTS pipeline_run (
  id                uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
 
  -- Provider identity
  provider          varchar(20)   NOT NULL,
  owner             varchar(200)  NOT NULL,
  repo              varchar(200)  NOT NULL,
  provider_run_id   varchar(100)  NOT NULL,  
  workflow_name     varchar(300),
 
  -- Run state
  status            varchar(30)   NOT NULL,   
  conclusion        varchar(30),              
 

  head_sha          varchar(64),
  branch            varchar(200),
 
  -- Timing
  started_at        timestamptz,
  completed_at      timestamptz,
  total_duration_ms bigint,
 
  created_at        timestamptz   NOT NULL DEFAULT now(),
  updated_at        timestamptz   NOT NULL DEFAULT now(),
 
  CONSTRAINT uq_pipeline_run UNIQUE (provider, owner, repo, provider_run_id)
);
 
CREATE INDEX idx_pipeline_run_provider_repo ON pipeline_run (provider, owner, repo, branch);
CREATE INDEX idx_pipeline_run_created_at    ON pipeline_run (created_at DESC);