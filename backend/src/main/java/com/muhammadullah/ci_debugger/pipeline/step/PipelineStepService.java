package com.muhammadullah.ci_debugger.pipeline.step;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
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
     * <p>Steps are saved in the order provided. If any step fails to save,
     * the entire batch is rolled back — partial step data for a run is
     * worse than no step data at all.
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