create table if not exists step_log (
  id uuid primary key default gen_random_uuid(),

  pipeline_step_id uuid not null references pipeline_step(id) on delete cascade,

  storage_type varchar(20) not null default 'DB',
  storage_key varchar(500),
  log_text text,

  created_at timestamptz not null default now()
);