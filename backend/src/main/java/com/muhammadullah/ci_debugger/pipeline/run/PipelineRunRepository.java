package com.muhammadullah.ci_debugger.pipeline.run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {

    Optional<PipelineRun> findByProviderAndOwnerAndRepoAndProviderRunId(
            PipelineRunProvider provider,
            String owner,
            String repo,
            String providerRunId
    );

    /**
     * Returns the 5 most recent runs per workflow, across all repos.
     *
     */
    @Query(value = """
            SELECT id, provider, owner, repo, provider_run_id, workflow_name,
                   status, conclusion, head_sha, branch,
                   started_at, completed_at, total_duration_ms,
                   created_at, updated_at
            FROM (
                SELECT *,
                       ROW_NUMBER() OVER (
                           PARTITION BY owner, repo, workflow_name
                           ORDER BY created_at DESC
                       ) AS rn
                FROM pipeline_run
            ) ranked
            WHERE rn <= 5
            ORDER BY owner ASC, repo ASC, workflow_name ASC, created_at DESC
            """, nativeQuery = true)
    List<PipelineRun> findRecentRunsPerWorkflow();

}