package com.muhammadullah.ci_debugger.pipeline.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.pullrequest.PullRequest;
import com.muhammadullah.ci_debugger.pipeline.run.dto.CommitRunSetResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoStatusResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RepoSummaryResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.RunSummaryResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.WorkflowSummaryResponse;

@Service
public class PipelineRunService {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunService.class);

    private static final int REPO_PAGE_SIZE = 20;
    private static final int COMMIT_SET_PAGE_SIZE = 10;

    private final PipelineRunRepository repository;

    public PipelineRunService(PipelineRunRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates or updates a pipeline run identified by
     * {@code (provider, owner, repo, providerRunId)}.
     *
     * <p>
     * Safe to call multiple times for the same run — designed to handle
     * successive GitHub webhooks ({@code requested}, {@code in_progress},
     * {@code completed}) for the same workflow run.
     *
     * @param req the upsert request
     * @return the created or updated run
     * @throws ServiceException with {@link ErrorCode#PROVIDER_NOT_SUPPORTED} if
     *                          the provider is unrecognised
     * @throws ServiceException with {@link ErrorCode#DB_UPSERT_FAILED} if an
     *                          unexpected database error occurs
     */
    @Transactional
    public PipelineRunResponse upsert(PipelineRunUpsertRequest req) {
        PipelineRunProvider provider = normalizeProvider(req.getProvider(), req.getProviderRunId());
        String owner = req.getOwner().trim();
        String repo = req.getRepo().trim();

        PipelineRunStatus status = coerceStatus(req.getStatus());
        PipelineRunConclusion conclusion = PipelineRunValueMapper.toConclusion(req.getConclusion());

        try {
            PipelineRun run = repository.findByProviderAndOwnerAndRepoAndProviderRunId(
                    provider, owner, repo, req.getProviderRunId())
                    .map(existing -> applyUpdate(existing, req, status, conclusion))
                    .orElseGet(() -> createNew(req, provider, owner, repo, status, conclusion));

            return PipelineRunResponse.from(run);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                    .addDetail("provider", provider)
                    .addDetail("owner", owner)
                    .addDetail("repo", repo)
                    .addDetail("providerRunId", req.getProviderRunId())
                    .addDetail("cause", e.getMessage());
        }
    }

    /**
     * Returns all repos grouped by owner → repo → workflowName, with the
     * 5 most recent runs per workflow.
     *
     * @return a list of repo summaries, each containing their workflow summaries
     */
    @Transactional(readOnly = true)
    public List<RepoSummaryResponse> listGrouped() {
        List<PipelineRun> runs = repository.findRecentRunsPerWorkflow();

        return runs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getOwner() + "/" + r.getRepo(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<PipelineRun> repoRuns = entry.getValue();
                    String owner = repoRuns.get(0).getOwner();
                    String repo = repoRuns.get(0).getRepo();

                    List<WorkflowSummaryResponse> workflows = repoRuns.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    r -> r.getWorkflowName() != null ? r.getWorkflowName() : "",
                                    java.util.LinkedHashMap::new,
                                    java.util.stream.Collectors.toList()))
                            .entrySet().stream()
                            .map(wEntry -> new WorkflowSummaryResponse(
                                    wEntry.getKey(),
                                    wEntry.getValue().stream()
                                            .map(RunSummaryResponse::from)
                                            .toList()))
                            .toList();

                    return new RepoSummaryResponse(owner, repo, workflows);
                })
                .toList();
    }

    @Transactional
    public void linkPullRequest(UUID pipelineRunId, PullRequest pullRequest) {
        PipelineRun run = repository.findById(pipelineRunId)
                .orElseThrow(() -> ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                        .addDetail("pipelineRunId", pipelineRunId));
        run.setPullRequest(pullRequest);
        repository.save(run);
        log.info("Linked PR {} to pipeline run {}", pullRequest.getId(), pipelineRunId);
    }

    /**
     * Returns a paginated list of runs for a specific repo, sorted by
     * {@code createdAt DESC}.
     *
     * @param owner the repository owner
     * @param repo  the repository name
     * @param page  zero-based page number
     * @return a page of run summaries for the given repo
     */
    @Transactional(readOnly = true)
    public Page<RunSummaryResponse> listByRepo(String owner, String repo, int page) {
        return repository.findMainBranchRunsByOwnerAndRepo(owner, repo, PageRequest.of(page, REPO_PAGE_SIZE))
                .map(RunSummaryResponse::from);
    }

    /**
     * Returns a single pipeline run by ID.
     *
     * @param id the pipeline run ID
     * @return the run as a full response
     * @throws ServiceException with {@link ErrorCode#PIPELINE_RUN_NOT_FOUND} if
     *                          no run exists for the given ID
     */
    @Transactional(readOnly = true)
    public PipelineRunResponse findById(UUID id) {
        return repository.findById(id)
                .map(PipelineRunResponse::from)
                .orElseThrow(() -> {
                    log.warn("Pipeline run {} not found", id);
                    return ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                            .addDetail("id", id);
                });
    }

    /**
     * Returns a summary of each repo's overall health based on the latest
     * main branch run per workflow.
     *
     * @return a list of repo statuses ordered by owner and repo name
     */
    @Transactional(readOnly = true)
    public List<RepoStatusResponse> listRepoStatuses() {
        return repository.findRepoHealthSummaries().stream()
                .map(RepoStatusResponse::from)
                .toList();
    }

    private PipelineRun createNew(
            PipelineRunUpsertRequest req,
            PipelineRunProvider provider,
            String owner,
            String repo,
            PipelineRunStatus status,
            PipelineRunConclusion conclusion) {
        PipelineRun run = new PipelineRun(provider, owner, repo, req.getProviderRunId(), status);

        applyMetadata(run, req);

        if (req.getStartedAt() != null) {
            run.markStarted(req.getStartedAt());
        }

        applyCompletion(run, req, conclusion);

        PipelineRun savedPipelineRun = repository.save(run);
        log.info("Created pipeline run {} for {}/{} providerRunId={} status={}",
                savedPipelineRun.getId(), owner, repo, req.getProviderRunId(), status);
        return savedPipelineRun;
    }

    /**
     * Returns a paginated list of main branch commit run sets for a given repo.
     *
     * @param owner the repository owner
     * @param repo  the repository name
     * @param page  zero-based page number
     * @return a page of commit run sets ordered by most recent commit first
     */
    @Transactional(readOnly = true)
    public Page<CommitRunSetResponse> listMainBranchRunSets(String owner, String repo, int page) {
        List<String> allHeadShas = repository.findDistinctMainBranchHeadShas(owner, repo);

        if (allHeadShas.isEmpty()) {
            return Page.empty();
        }

        int total = allHeadShas.size();
        int fromIndex = page * COMMIT_SET_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + COMMIT_SET_PAGE_SIZE, total);

        if (fromIndex >= total) {
            return Page.empty();
        }

        List<String> pageHeadShas = allHeadShas.subList(fromIndex, toIndex);
        List<PipelineRun> runs = repository.findMainBranchRunsByHeadShaIn(owner, repo, pageHeadShas);

        Map<String, List<PipelineRun>> grouped = runs.stream()
                .collect(Collectors.groupingBy(PipelineRun::getHeadSha));

        List<CommitRunSetResponse> sets = pageHeadShas.stream()
                .map(sha -> {
                    List<PipelineRun> shaRuns = grouped.getOrDefault(sha, List.of());
                    Instant startedAt = shaRuns.stream()
                            .map(PipelineRun::getStartedAt)
                            .filter(t -> t != null)
                            .min(Instant::compareTo)
                            .orElse(null);
                    return CommitRunSetResponse.from(sha, startedAt, shaRuns);
                })
                .toList();

        return new PageImpl<>(sets, PageRequest.of(page, COMMIT_SET_PAGE_SIZE), total);
    }

    private PipelineRun applyUpdate(
            PipelineRun existing,
            PipelineRunUpsertRequest req,
            PipelineRunStatus incomingStatus,
            PipelineRunConclusion incomingConclusion) {
        existing.setStatus(incomingStatus);

        if (req.getStartedAt() != null && existing.getStartedAt() == null) {
            existing.setStartedAt(req.getStartedAt());
        }

        applyMetadata(existing, req);
        applyCompletion(existing, req, incomingConclusion);

        PipelineRun savedPipelineRun = repository.save(existing);
        log.info("Updated pipeline run {} for {}/{} providerRunId={} status={}",
                savedPipelineRun.getId(), existing.getOwner(), existing.getRepo(), existing.getProviderRunId(),
                incomingStatus);

        return savedPipelineRun;
    }

    private void applyMetadata(PipelineRun run, PipelineRunUpsertRequest req) {
        if (req.getWorkflowName() != null && run.getWorkflowName() == null) {
            run.setWorkflowName(req.getWorkflowName());
        }
        if (req.getHeadSha() != null && run.getHeadSha() == null) {
            run.setHeadSha(req.getHeadSha());
        }
        if (req.getBranch() != null && run.getBranch() == null) {
            run.setBranch(req.getBranch());
        }
    }

    private void applyCompletion(PipelineRun run, PipelineRunUpsertRequest req, PipelineRunConclusion conclusion) {
        if (req.getCompletedAt() != null) {
            long duration = coerceDuration(req.getTotalDurationMs(), run.getStartedAt(), req.getCompletedAt());
            run.markCompleted(conclusion, req.getCompletedAt(), duration);
        }
    }

    private PipelineRunStatus coerceStatus(String raw) {
        if (raw == null) {
            return PipelineRunStatus.UNKNOWN;
        }

        String trimmed = raw.trim();
        PipelineRunStatus mapped = PipelineRunValueMapper.toStatus(trimmed);

        if (mapped != PipelineRunStatus.UNKNOWN) {
            return mapped;
        }

        try {
            return PipelineRunStatus.valueOf(trimmed.toUpperCase());
        } catch (Exception e) {
            return PipelineRunStatus.UNKNOWN;
        }
    }

    private PipelineRunProvider normalizeProvider(String provider, String providerRunId) {
        try {
            return PipelineRunProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported provider '{}' for providerRunId={}", provider, providerRunId);
            throw ServiceException.of(ErrorCode.PROVIDER_NOT_SUPPORTED)
                    .addDetail("provider", provider)
                    .addDetail("supportedProviders", PipelineRunProvider.values())
                    .addDetail("providerRunId", providerRunId);
        }
    }

    private long coerceDuration(Long provided, Instant startedAt, Instant completedAt) {
        if (provided != null && provided >= 0) {
            return provided;
        }
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        long ms = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        return Math.max(0, ms);
    }
}
