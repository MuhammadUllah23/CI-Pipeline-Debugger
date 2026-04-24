package com.muhammadullah.ci_debugger.pipeline.step;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunRepository;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepResponse;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PipelineStepService {

    private static final Logger log = LoggerFactory.getLogger(PipelineStepService.class);

    private final PipelineStepRepository stepRepository;
    private final PipelineRunRepository runRepository;

    public PipelineStepService(
            PipelineStepRepository stepRepository,
            PipelineRunRepository runRepository
    ) {
        this.stepRepository = stepRepository;
        this.runRepository = runRepository;
    }

    /**
     * Persists a batch of steps for the given pipeline run.
     *
     * @param pipelineRunId the ID of the pipeline run these steps belong to
     * @param requests      the list of steps to save
     * @return the saved steps as responses
     * @throws ServiceException with {@link ErrorCode#PIPELINE_RUN_NOT_FOUND} if
     *                          no run exists for the given ID
     * @throws ServiceException with {@link ErrorCode#DB_UPSERT_FAILED} if an
     *                          unexpected database error occurs
     */
    @Transactional
    public List<PipelineStepResponse> saveAll(UUID pipelineRunId, List<PipelineStepSaveRequest> requests) {
        PipelineRun run = runRepository.findById(pipelineRunId)
                .orElseThrow(() -> {
                    log.warn("Cannot save steps — pipeline run {} not found", pipelineRunId);
                    return ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                            .addDetail("pipelineRunId", pipelineRunId);
                });

        try {
            List<PipelineStep> steps = requests.stream()
                    .map(request -> buildStep(run, request))
                    .toList();

            List<PipelineStep> savedSteps = stepRepository.saveAll(steps);
            log.info("Saved {} steps for pipeline run {}", savedSteps.size(), pipelineRunId);

            return savedSteps.stream()
                    .map(PipelineStepResponse::from)
                    .toList();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to save steps for pipeline run {} — {}", pipelineRunId, e.getMessage());
            throw ServiceException.of(ErrorCode.DB_UPSERT_FAILED)
                    .addDetail("pipelineRunId", pipelineRunId)
                    .addDetail("cause", e.getMessage());
        }
    }

    /**
     * Returns all steps for the given pipeline run whose conclusion indicates
     * a failure. Used by job handlers to determine which steps need error clustering.
     *
     * @param pipelineRunId the ID of the pipeline run to fetch failed steps for
     * @return all failed steps for the run, or an empty list if none exist
     */
    public List<PipelineStep> findFailedSteps(UUID pipelineRunId) {
        return stepRepository.findByPipelineRunIdAndConclusionIn(
                pipelineRunId, PipelineRunConclusion.FAILURE_CONCLUSIONS);
    }

    /**
     * Returns all steps for a given pipeline run ordered by job name
     * then step index — the natural reading order for the dashboard.
     *
     * @param pipelineRunId the ID of the pipeline run to fetch steps for
     * @return steps ordered by job_name ASC, step_index ASC
     * @throws ServiceException with {@link ErrorCode#PIPELINE_RUN_NOT_FOUND} if
     *                          no run exists for the given ID
     */
    @Transactional(readOnly = true)
    public List<PipelineStepResponse> getStepsForRun(UUID pipelineRunId) {
        if (!runRepository.existsById(pipelineRunId)) {
            log.warn("Cannot fetch steps — pipeline run {} not found", pipelineRunId);
            throw ServiceException.of(ErrorCode.PIPELINE_RUN_NOT_FOUND)
                    .addDetail("pipelineRunId", pipelineRunId);
        }

        return stepRepository.findByPipelineRunIdOrderByJobNameAscStepIndexAsc(pipelineRunId)
                .stream()
                .map(PipelineStepResponse::from)
                .toList();
    }

    
    private PipelineStep buildStep(PipelineRun run, PipelineStepSaveRequest request) {
        PipelineStep step = new PipelineStep(
                run,
                request.getJobName(),
                request.getStepName(),
                request.getStepIndex()
        );

        step.applyCompletion(
                request.getStatus(),
                request.getConclusion(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getDurationMs()
        );

        return step;
    }
}