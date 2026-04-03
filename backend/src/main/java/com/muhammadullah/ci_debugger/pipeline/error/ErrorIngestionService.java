package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.step.PipelineStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ErrorIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ErrorIngestionService.class);

    private final ErrorClusterRepository clusterRepository;
    private final ErrorOccurrenceRepository occurrenceRepository;
    private final PipelineRunRepository runRepository;

    public ErrorIngestionService(
            ErrorClusterRepository clusterRepository,
            ErrorOccurrenceRepository occurrenceRepository,
            PipelineRunRepository runRepository
    ) {
        this.clusterRepository = clusterRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.runRepository = runRepository;
    }

    /**
     * Ingests error data for a completed pipeline run by creating or updating
     * clusters and saving occurrences for each failed step.
     *
     * @param pipelineRunId the ID of the run whose errors should be ingested
     * @param snippetByStep a map of failed step to its extracted error snippet,
     *                      snippet may be null if no error lines were found
     * @throws ServiceException with {@link ErrorCode#PIPELINE_RUN_NOT_FOUND} if
     *                          no run exists for the given ID
     * @throws ServiceException with {@link ErrorCode#DB_UPSERT_FAILED} if an
     *                          unexpected database error occurs
     */
    @Transactional
    public void ingestErrors(UUID pipelineRunId, Map<PipelineStep, String> snippetByStep) {
        PipelineRun run = runRepository.findById(pipelineRunId)
                .orElseThrow(() -> {
                    log.warn("Cannot ingest errors — pipeline run {} not found", pipelineRunId);
                    return ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                            .addDetail("pipelineRunId", pipelineRunId);
                });

        try {

            for (Map.Entry<PipelineStep, String> entry : snippetByStep.entrySet()) {
                ingestStep(run, entry.getKey(), entry.getValue());
            }

            log.info("Ingested {} failed step(s) for pipeline run {}",
                    snippetByStep.size(), pipelineRunId);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to ingest errors for pipeline run {} — {}", pipelineRunId, e.getMessage());
            throw ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                    .addDetail("pipelineRunId", pipelineRunId)
                    .addDetail("cause", e.getMessage());
        }
    }

    //** ── private helpers ────────────────────────────────────────────────────

    private void ingestStep(PipelineRun run, PipelineStep step, String snippet) {
        String conclusionName = step.getConclusion().name();

        String fingerprint = computeFingerprint(
                run.getOwner(), run.getRepo(),
                step.getJobName(), step.getStepName(), conclusionName);

        ErrorCluster cluster = clusterRepository.findByFingerprint(fingerprint)
                .map(existing -> {
                    existing.recordOccurrence();
                    return clusterRepository.save(existing);
                })
                .orElseGet(() -> {
                    ErrorCluster newCluster = new ErrorCluster(
                            fingerprint,
                            run.getOwner(),
                            run.getRepo(),
                            step.getJobName(),
                            step.getStepName(),
                            conclusionName
                    );
                    if (snippet != null) {
                        newCluster.setRepresentativeMessage(snippet);
                    }
                    return clusterRepository.save(newCluster);
                });

        if (!occurrenceRepository.existsByErrorClusterAndPipelineRun(cluster, run)) {
            ErrorOccurrence occurrence = new ErrorOccurrence(cluster, run, step);
            occurrence.setSnippet(snippet);
            occurrenceRepository.save(occurrence);
        } else {
            log.debug("Occurrence already exists for cluster {} and run {} — skipping",
                    cluster.getId(), run.getId());
        }
    }

    /**
     * Computes a stable SHA-256 fingerprint for a
     * (owner, repo, jobName, stepName, conclusion) tuple.
     * The result is 64 hex characters — well within the varchar(128) column.
     */
    private String computeFingerprint(
            String owner, String repo, String jobName, String stepName, String conclusion) {
        String input = String.join("|",
                owner.toLowerCase(Locale.ROOT),
                repo.toLowerCase(Locale.ROOT),
                jobName != null ? jobName.toLowerCase(Locale.ROOT) : "",
                stepName.toLowerCase(Locale.ROOT),
                conclusion.toLowerCase(Locale.ROOT)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}