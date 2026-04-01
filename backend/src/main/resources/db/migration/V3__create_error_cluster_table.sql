CREATE TABLE IF NOT EXISTS error_cluster (
  id                     uuid          PRIMARY KEY DEFAULT gen_random_uuid(),

  fingerprint            varchar(128)  NOT NULL UNIQUE,

  owner                  varchar(200)  NOT NULL,
  repo                   varchar(200)  NOT NULL,
  job_name               varchar(300),
  step_name              varchar(300)  NOT NULL,
  conclusion             varchar(30)   NOT NULL,

  representative_message text,

  occurrence_count       bigint        NOT NULL DEFAULT 0,
  first_seen_at          timestamptz   NOT NULL DEFAULT now(),
  last_seen_at           timestamptz   NOT NULL DEFAULT now(),

  created_at             timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX idx_error_cluster_repo        ON error_cluster (owner, repo);
CREATE INDEX idx_error_cluster_last_seen   ON error_cluster (last_seen_at DESC);
CREATE INDEX idx_error_cluster_occurrence  ON error_cluster (occurrence_count DESC);