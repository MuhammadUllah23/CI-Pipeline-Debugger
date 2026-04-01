package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ErrorOccurrenceRepository extends JpaRepository<ErrorOccurrence, UUID> {

    /**
     * Guards against duplicate occurrences if CLUSTER_ERRORS is ever retried
     * for an already-clustered run.
     */
    boolean existsByErrorClusterAndPipelineRun(ErrorCluster errorCluster, PipelineRun pipelineRun);
}