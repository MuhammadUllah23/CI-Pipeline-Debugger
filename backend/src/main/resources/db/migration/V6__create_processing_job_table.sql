create table if not exists error_occurrence (
  id uuid primary key default gen_random_uuid(),

  error_cluster_id uuid not null references error_cluster(id) on delete cascade,
  pipeline_run_id uuid not null references pipeline_run(id) on delete cascade,
  pipeline_step_id uuid references pipeline_step(id) on delete set null,

  line_no int,
  snippet text,

  created_at timestamptz not null default now()
);