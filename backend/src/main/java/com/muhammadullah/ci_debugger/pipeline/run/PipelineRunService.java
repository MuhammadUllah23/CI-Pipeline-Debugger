package com.muhammadullah.ci_debugger.pipeline.run;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunResponse;
import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;

@Service
public class PipelineRunService {
    private final PipelineRunRepository repository;

    public PipelineRunService(PipelineRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PipelineRunResponse upsert(PipelineRunUpsertRequest req) {
        PipelineRunProvider provider = normalizeProvider(req.getProvider(), req.getProviderRunId());
        String owner = req.getOwner().trim();
        String repo = req.getRepo().trim();

        PipelineRunStatus status = coerceStatus(req.getStatus());
        PipelineRunConclusion conclusion = PipelineRunValueMapper.toConclusion(req.getConclusion());

        try {
            PipelineRun run = repository.findByProviderAndOwnerAndRepoAndProviderRunId(
                            provider, owner, repo, req.getProviderRunId()
                    )
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

    private PipelineRun createNew(
            PipelineRunUpsertRequest req,
            PipelineRunProvider provider,
            String owner,
            String repo,
            PipelineRunStatus status,
            PipelineRunConclusion conclusion
    ) {
        PipelineRun run = new PipelineRun(provider, owner, repo, req.getProviderRunId(), status);

        applyMetadata(run, req);

        if (req.getStartedAt() != null) {
            run.markStarted(req.getStartedAt());
        }

        applyCompletion(run, req, conclusion);

        return repository.save(run);
    }

    private PipelineRun applyUpdate(
            PipelineRun existing,
            PipelineRunUpsertRequest req,
            PipelineRunStatus incomingStatus,
            PipelineRunConclusion incomingConclusion
    ) {
        existing.setStatus(incomingStatus);

        if (req.getStartedAt() != null && existing.getStartedAt() == null) {
            existing.setStartedAt(req.getStartedAt());
        }

        applyMetadata(existing, req);
        applyCompletion(existing, req, incomingConclusion);

        return repository.save(existing);
    }

    private void applyMetadata(PipelineRun run, PipelineRunUpsertRequest req) {
        if (req.getWorkflowName() != null && run.getWorkflowName() == null) run.setWorkflowName(req.getWorkflowName());
        if (req.getHeadSha() != null && run.getHeadSha() == null) run.setHeadSha(req.getHeadSha());
        if (req.getBranch() != null && run.getBranch() == null) run.setBranch(req.getBranch());
    }

    private void applyCompletion(PipelineRun run, PipelineRunUpsertRequest req, PipelineRunConclusion conclusion) {
        if (req.getCompletedAt() != null) {
            long duration = coerceDuration(req.getTotalDurationMs(), run.getStartedAt(), req.getCompletedAt());
            run.markCompleted(conclusion, req.getCompletedAt(), duration);
        }
    }

    private PipelineRunStatus coerceStatus(String raw) {
        if (raw == null) return PipelineRunStatus.UNKNOWN;

        String trimmed = raw.trim();
        PipelineRunStatus mapped = PipelineRunValueMapper.toStatus(trimmed);

        if (mapped != PipelineRunStatus.UNKNOWN) return mapped;

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
            throw ServiceException.of(ErrorCode.PROVIDER_NOT_SUPPORTED)
                    .addDetail("provider", provider)
                    .addDetail("supportedProviders", PipelineRunProvider.values())
                    .addDetail("providerRunId", providerRunId);
        }
    }

    private long coerceDuration(Long provided, Instant startedAt, Instant completedAt) {
        if (provided != null && provided >= 0) return provided;
        if (startedAt == null || completedAt == null) return 0;
        long ms = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        return Math.max(0, ms);
    }
}

