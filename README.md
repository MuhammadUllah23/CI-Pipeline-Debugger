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
- Provider-specific job types — prevents handler map conflicts when GitLab/CircleCI support is added
- Single configured GitHub API token for MVP — per-user OAuth deferred to a future phase

---

### ✅ Phase 3 — Error Clustering

Groups recurring step failures across runs to surface patterns and repeated errors.

**What's included:**
- `error_cluster` table — recurring failure patterns identified by `owner + repo + job_name + step_name + conclusion` with SHA-256 fingerprint
- `error_occurrence` table — links a cluster to a specific pipeline run and step with log snippet
- `GitHubLogsApiClient` — downloads GitHub Actions log zip, unzips in memory, extracts `[ERROR]` and `##[error]` prefixed lines
- `ErrorIngestionService` — provider-agnostic, finds or creates clusters via fingerprint, saves occurrences in one transaction
- Single combined job type `GITHUB_FETCH_LOGS_AND_CLUSTER` — triggered after step fetch completes on failed runs

**Key design decisions:**
- Single combined job type avoids data handoff problem between async jobs
- `ErrorIngestionService` signature uses `Map<PipelineStep, String>` — provider-agnostic
- Log parsing extracts error lines only — full logs discarded after parsing, not stored
- `step_log` table dropped — log parsing too unstructured to be worth storing raw logs

---

### ✅ Phase 4 — REST API

Exposes pipeline run, step, and error cluster data via a REST API for consumption by the dashboard frontend.

**What's included:**
- `GET /api/runs` — all repos grouped by owner → repo → workflowName, 5 most recent runs per workflow using PostgreSQL ROW_NUMBER() window function
- `GET /api/runs/{owner}/{repo}` — paginated flat list of runs for a specific repo, 20 per page, sorted by `created_at DESC`
- `GET /api/runs/{id}` — single run detail
- `GET /api/runs/{id}/steps` — all steps for a run ordered by `job_name ASC`, `step_index ASC`
- `GET /api/runs/{id}/clusters` — error clusters triggered by a specific run
- `GET /api/clusters` — all clusters sorted by `occurrence_count DESC`, configurable limit clamped to 100
- `GET /api/clusters/{id}` — single cluster detail with all occurrences
- Full unit test coverage across all new service methods

**Key design decisions:**
- Backend handles grouping and pagination — not the frontend
- `RunSummaryResponse` as a lean DTO for list views — full `PipelineRunResponse` reserved for detail endpoint
- `ErrorClusterWithOccurrencesResponse` for cluster detail — occurrences not loaded on list endpoints
- `readOnly = true` on all read service methods — skips Hibernate dirty checking

---

### 🔲 Phase 5 — Pull Request Tracking & Branch Status

Extends pipeline run ingestion to capture pull request metadata, enabling PR-level views on the dashboard.

**Planned:**
- New columns on `pipeline_run`: `pr_number`, `pr_title`, `pr_state`
- `GitHubWebhookPayload` updated to deserialize the `pull_requests` array from the webhook payload
- PR info extracted in `GitHubWebhookMapper` and carried through `PipelineRunUpsertRequest`
- New endpoints:
  - `GET /api/runs/pull-requests?owner=&repo=` — all open PR runs for a repo
  - `GET /api/runs/main-status?owner=&repo=` — last run on main branch
  - `GET /api/runs/last-merged?owner=&repo=` — most recently merged PR for a repo
- CI workflow updated to trigger on pull requests targeting main

---

### 🔲 Phase 6 — Dashboard Frontend

Visual frontend for exploring pipeline runs, step timelines, and error clusters.

**Planned:**
- React dashboard with:
  - Home page — repos grouped by workflow with 5 most recent runs each
  - Repo detail page — paginated run history, main branch status, last merged PR, open PRs
  - Run detail page — step breakdown, error clusters, log snippets
  - Error clusters page — most frequent failures sorted by occurrence count
- Real-time step progress via WebSocket (`RunProgressNotifier` real implementation)

---

### 🔲 Phase 7 — GitHub OAuth

Adds per-user authentication and token management.

**Planned:**
- GitHub OAuth2 login
- Per-user GitHub API token storage
- Owner-scoped queries across all endpoints
- Per-installation webhook secret management

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
- **GitHub only** — GitLab and CircleCI deferred to Phase 8
