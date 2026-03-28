# CI Pipeline Visual Debugger

A full-stack observability tool for CI/CD pipelines. Ingests GitHub Actions webhook data, processes pipeline run and step-level information, clusters recurring errors, and presents results through a visual dashboard.

Built to demonstrate full-stack and backend engineering skills including REST API design, asynchronous job processing, database schema design, and provider-agnostic architecture.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 15 |
| Migrations | Flyway (Spring Boot managed) |
| ORM | Hibernate / Spring Data JPA |
| Containerization | Docker / Docker Compose |
| Frontend | React (planned) |

---

## Architecture Overview

```
GitHub Webhook
      │
      ▼
GitHubWebhookController     — validates HMAC signature, deserializes payload
      │
      ▼
PipelineRunService          — upserts pipeline run (idempotent)
      │  action == completed
      ▼
ProcessingJobService        — enqueues FETCH_STEPS job
      │
      · · · @Scheduled poller · · ·
      │
      ▼
GitHubStepsApiClient        — fetches step data from GitHub REST API
      │
      ▼
PipelineStepService         — persists step rows to database
```

---

## Phases

### ✅ Phase 1 — GitHub Webhook Ingestion

Establishes the webhook ingestion pipeline for GitHub Actions `workflow_run` events.

**What's included:**
- `POST /webhooks/github` endpoint
- HMAC-SHA256 signature verification via `X-Hub-Signature-256` header
- Payload deserialization and mapping to internal `PipelineRunUpsertRequest`
- Idempotent upsert of `pipeline_run` rows — safe to call on `requested`, `in_progress`, and `completed` events for the same run
- Write-once metadata policy — `workflowName`, `headSha`, `branch`, and `startedAt` are never overwritten by a later webhook
- Provider-agnostic service layer — `PipelineRunService` has no knowledge of GitHub specifics
- Exception handling via `ServiceException` with typed `ErrorCode` enum and `GlobalExceptionHandler`
- Dev-only `PipelineRunDevController` for seeding and inspecting run data

**Key design decisions:**
- One controller per provider (not a unified webhook controller)
- `provider_run_id` stored as `varchar(100)` to support non-numeric IDs from future providers

---

### ✅ Phase 2 — Step-Level Data Ingestion

Adds asynchronous background processing to fetch and persist step-level data from the GitHub API after a run completes.

**What's included:**
- `processing_job` table with retry logic, exponential backoff, and partial unique index on active jobs
- `pipeline_step` table with unique constraint on `(pipeline_run_id, job_name, step_index)`
- Idempotent `ProcessingJobService.enqueue()` — returns existing job if one is already active or completed for the same run
- `@Scheduled` job processor polling for `PENDING` jobs on a configurable interval
- `JobHandler` interface with `GitHubFetchStepsJobHandler` implementation — extensible for future providers
- `GitHubStepsApiClient` — calls `GET /repos/{owner}/{repo}/actions/runs/{runId}/jobs` with configurable connect and read timeouts
- Exponential backoff retry: 30s → 90s → 270s, max 3 attempts
- Jobs marked `FAILED` permanently after all attempts exhausted — visible in dashboard for manual investigation
- `RunProgressNotifier` interface with `NoOpRunProgressNotifier` for MVP — extension point for future real-time WebSocket updates
- Full unit test coverage across all service, repository, and API client classes

**Key design decisions:**
- Step data only fetched on `completed` webhook — GitHub step data is incomplete mid-run
- `scheduled_at` immutable — records original enqueue time; `next_retry_at` handles retry delay separately
- `JOIN FETCH` on eligible jobs query — avoids lazy loading proxy errors outside transaction scope
- Provider-specific job types (`GITHUB_FETCH_STEPS`) — prevents handler map conflicts when GitLab/CircleCI support is added
- Single configured GitHub API token for MVP — per-user OAuth deferred to a future phase

---

### 🔲 Phase 3 — Error Clustering

Groups recurring step failures across runs to surface patterns and repeated errors.

**Planned:**
- `ErrorClusterService` — groups failures by step name, error message similarity, and repository
- `ErrorClusterController` — exposes cluster data via REST API
- Background job type `CLUSTER_ERRORS` — triggered after step data is persisted
- Dashboard view showing most frequent failure patterns

---

### 🔲 Phase 4 — Dashboard

Visual frontend for exploring pipeline runs, step timelines, and error clusters.

**Planned:**
- React dashboard with run list, step-level drill-down, and error cluster views
- Real-time step progress via WebSocket (`RunProgressNotifier` real implementation)
- GitHub OAuth authentication — per-user token management
- Token URL approach for sharing with early testers

---

### 🔲 Phase 5 — Additional Providers

Extends ingestion and step fetching to GitLab and CircleCI.

**Planned:**
- `GitLabWebhookController` and `CircleCiWebhookController`
- Provider-specific `JobHandler` implementations (`GitLabFetchStepsJobHandler`, `CircleCiFetchStepsJobHandler`)
- Provider-agnostic `PipelineRunService` already in place — minimal changes required

---

## MVP Limitations

The following are known limitations consciously deferred in favour of a simpler MVP:

- **Step data loss** — if a `FETCH_STEPS` job fails all 3 retries, step data is lost for that run unless manually re-enqueued
- **Single GitHub API token** — not per-user; OAuth deferred to a future phase
- **No stuck job recovery** — jobs stuck in `IN_PROGRESS` are never automatically reset
- **No dead letter queue** — permanently failed jobs require manual database intervention
- **Log storage in PostgreSQL** — S3 explicitly ruled out for MVP
- **No real-time updates** — dashboard requires polling; `RunProgressNotifier` is `NoOp`
- **No pre-computed performance snapshots** — all metrics derived at query time
- **GitHub only** — GitLab and CircleCI deferred to Phase 5
