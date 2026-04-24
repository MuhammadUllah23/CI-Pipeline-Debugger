package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.exception.ErrorCode;
import com.muhammadullah.ci_debugger.exception.ServiceException;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterResponse;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterWithOccurrencesResponse;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorOccurrenceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ErrorClusterService {

    private static final Logger log = LoggerFactory.getLogger(ErrorClusterService.class);
    private static final int MAX_CLUSTER_LIMIT = 100;

    private final ErrorClusterRepository clusterRepository;
    private final ErrorOccurrenceRepository occurrenceRepository;

    public ErrorClusterService(
            ErrorClusterRepository clusterRepository,
            ErrorOccurrenceRepository occurrenceRepository
    ) {
        this.clusterRepository = clusterRepository;
        this.occurrenceRepository = occurrenceRepository;
    }

    /**
     * Returns all clusters sorted by occurrence count descending.
     *
     * @param limit the maximum number of clusters to return
     * @return clusters ordered by occurrence_count DESC
     */
    @Transactional(readOnly = true)
    public List<ErrorClusterResponse> listAll(int limit) {
        int clampedLimit = Math.min(limit, MAX_CLUSTER_LIMIT);
        log.debug("Fetching top {} error clusters by occurrence count", clampedLimit);

        return clusterRepository
                .findAllByOrderByOccurrenceCountDesc(PageRequest.of(0, clampedLimit))
                .stream()
                .map(ErrorClusterResponse::from)
                .toList();
    }

    /**
     * Returns a single cluster by ID along with all its occurrences.
     *
     * @param id the cluster ID
     * @return the cluster with its occurrences
     * @throws ServiceException with {@link ErrorCode#ERROR_CLUSTER_NOT_FOUND} if
     *                          no cluster exists for the given ID
     */
    @Transactional(readOnly = true)
    public ErrorClusterWithOccurrencesResponse findById(UUID id) {
        ErrorCluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error cluster {} not found", id);
                    return ServiceException.of(ErrorCode.ERROR_CLUSTER_NOT_FOUND)
                            .addDetail("id", id);
                });

        List<ErrorOccurrenceResponse> occurrences = occurrenceRepository
                .findByErrorClusterIdOrderByCreatedAtDesc(id)
                .stream()
                .map(ErrorOccurrenceResponse::from)
                .toList();

        log.debug("Found cluster {} with {} occurrences", id, occurrences.size());
        return ErrorClusterWithOccurrencesResponse.of(cluster, occurrences);
    }

    /**
     * Returns all clusters triggered by a specific pipeline run.
     *
     * @param pipelineRunId the pipeline run ID
     * @return distinct clusters associated with the run, in occurrence order
     */
    @Transactional(readOnly = true)
    public List<ErrorClusterResponse> findByRunId(UUID pipelineRunId) {
        log.debug("Fetching error clusters for pipeline run {}", pipelineRunId);

        return clusterRepository.findByRunId(pipelineRunId)
                .stream()
                .map(ErrorClusterResponse::from)
                .toList();
        }
}