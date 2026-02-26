CREATE EXTENSION IF NOT EXISTS "pgcrypto";

create table IF not exists pipeline_run (
  id uuid primary key default gen_random_uuid(),

  provider varchar(20) not null default 'GITHUB',
  owner varchar(200) not null,
  repo varchar(200) not null,

  provider_run_id bigint not null,
  workflow_name varchar(300),
  status varchar(30) not null,
  conclusion varchar(30),

  head_sha varchar(64),
  branch varchar(200),

  started_at timestamptz,
  completed_at timestamptz,
  total_duration_ms bigint,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint uq_pipeline_run unique (provider, owner, repo, provider_run_id)
);