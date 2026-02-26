create table if not exists error_cluster (
  id uuid primary key default gen_random_uuid(),

  fingerprint varchar(128) not null unique,
  error_type varchar(60),
  representative_message text not null,

  occurrences bigint not null default 0,
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now(),

  created_at timestamptz not null default now()
);