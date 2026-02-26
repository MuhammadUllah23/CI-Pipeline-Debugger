create table if not exists pipeline_step (
  id uuid primary key default gen_random_uuid(),

  pipeline_run_id uuid not null references pipeline_run(id) on delete cascade,

  job_name varchar(300),
  step_name varchar(300) not null,
  step_index int not null,

  status varchar(30),
  conclusion varchar(30),

  started_at timestamptz,
  completed_at timestamptz,
  duration_ms bigint,

  created_at timestamptz not null default now(),

  constraint uq_step unique (pipeline_run_id, step_index, step_name)
);