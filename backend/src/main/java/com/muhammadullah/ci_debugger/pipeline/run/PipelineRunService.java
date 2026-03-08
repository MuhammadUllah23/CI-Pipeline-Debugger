package com.muhammadullah.ci_debugger.pipeline.run;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;

@Service
public class PipelineRunService {
    private final PipelineRunRepository repository;

    public PipelineRunService(PipelineRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PipelineRun upsert(PipelineRunUpsertRequest req) {
        String provider = normalizeProvider(req.getProvider());
        String owner = req.getOwner().trim();
        String repo = req.getRepo().trim();

        PipelineRunStatus status = coerceStatus(req.getStatus());
        PipelineRunConclusion conclusion = PipelineRunValueMapper.toConclusion(req.getConclusion());

        return repository.findByProviderAndOwnerAndRepoAndProviderRunId(
                        provider, owner, repo, req.getProviderRunId()
                )
                .map(existing -> applyUpdate(existing, req, status, conclusion))
                .orElseGet(() -> createNew(req, provider, owner, repo, status, conclusion));
    }

    private PipelineRun createNew(
            PipelineRunUpsertRequest req,
            String provider,
            String owner,
            String repo,
            PipelineRunStatus status,
            PipelineRunConclusion conclusion
    ) {
        PipelineRun run = new PipelineRun(provider, owner, repo, req.getProviderRunId(), status);

        if (req.getWorkflowName() != null) run.setWorkflowName(req.getWorkflowName());
        if (req.getHeadSha() != null) run.setHeadSha(req.getHeadSha());
        if (req.getBranch() != null) run.setBranch(req.getBranch());

        if (req.getStartedAt() != null) {
            run.markStarted(req.getStartedAt());
        }

        if (req.getCompletedAt() != null  && run.getStartedAt() != null) {
            long duration = coerceDuration(req.getTotalDurationMs(), run.getStartedAt(), req.getCompletedAt());
            run.markCompleted(conclusion, req.getCompletedAt(), duration);
        } 

        return repository.save(run);
    }

    private PipelineRun applyUpdate(
            PipelineRun existing,
            PipelineRunUpsertRequest req,
            PipelineRunStatus incomingStatus,
            PipelineRunConclusion incomingConclusion
    ) {

        return repository.save(existing);
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

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) return "GITHUB";
        return provider.trim().toUpperCase();
    }

    private long coerceDuration(Long provided, Instant startedAt, Instant completedAt) {
        if (provided != null && provided >= 0) return provided;
        if (startedAt == null || completedAt == null) return 0;
        long ms = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        return Math.max(0, ms);
    }
}

