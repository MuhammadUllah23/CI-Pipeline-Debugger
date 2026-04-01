ALTER TABLE error_cluster
    ADD COLUMN owner      varchar(200),
    ADD COLUMN repo       varchar(200),
    ADD COLUMN job_name   varchar(300),
    ADD COLUMN step_name  varchar(300),
    ADD COLUMN conclusion varchar(30);

CREATE INDEX idx_error_cluster_repo ON error_cluster (owner, repo);