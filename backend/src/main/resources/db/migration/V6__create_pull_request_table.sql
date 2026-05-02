CREATE TABLE IF NOT EXISTS pull_request (
  id               uuid          PRIMARY KEY DEFAULT gen_random_uuid(),

  provider         varchar(20)   NOT NULL,
  owner            varchar(200)  NOT NULL,
  repo             varchar(200)  NOT NULL,
  pr_number        int           NOT NULL,

  title            varchar(500),
  head_sha         varchar(64),
  head_branch      varchar(200),

  pr_state_raw     varchar(20),
  pr_state         varchar(20),

  created_at       timestamptz   NOT NULL DEFAULT now(),
  updated_at       timestamptz   NOT NULL DEFAULT now(),

  CONSTRAINT uq_pull_request UNIQUE (provider, owner, repo, pr_number)
);

CREATE INDEX idx_pull_request_owner_repo ON pull_request (provider, owner, repo);
CREATE INDEX idx_pull_request_state      ON pull_request (pr_state)
  WHERE pr_state = 'OPEN';