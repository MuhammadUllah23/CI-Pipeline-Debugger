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
            String providerRunId);

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

    /**
     * Returns a paginated list of runs for a specific repo, sorted by
     *
     * @return a page of runs for the given repo
     */
    @Query("""
            SELECT r FROM PipelineRun r
            WHERE r.owner = :owner
            AND r.repo = :repo
            AND r.branch = 'main'
            AND r.pullRequest IS NULL
            ORDER BY r.createdAt DESC
            """)
    Page<PipelineRun> findMainBranchRunsByOwnerAndRepo(
            @Param("owner") String owner,
            @Param("repo") String repo,
            Pageable pageable);

    /**
     * Returns paginated pipeline runs for a specific pull request.
     *
     * @param pullRequestId the ID of the pull request
     * @param pageable      pagination parameters
     * @return paginated runs for the PR
     */
    Page<PipelineRun> findByPullRequestIdOrderByCreatedAtDesc(UUID pullRequestId, Pageable pageable);

    /**
     * Returns the most recent pipeline run for each open pull request.
     */
    @Query(value = """
            SELECT r.* FROM pipeline_run r
            JOIN pull_request pr ON r.pr_id = pr.id
            WHERE pr.pr_state = 'OPEN'
            AND r.id = (
                SELECT r2.id FROM pipeline_run r2
                WHERE r2.pr_id = pr.id
                AND r2.workflow_name = r.workflow_name
                ORDER BY r2.created_at DESC
                LIMIT 1
            )
            ORDER BY r.created_at DESC
            """, nativeQuery = true)
    List<PipelineRun> findLatestRunPerWorkflowForOpenPullRequests();

    @Query(value = """
            SELECT owner, repo,
              CASE
                WHEN bool_or(conclusion = 'FAILURE') THEN 'FAILURE'
                WHEN bool_or(status != 'COMPLETED') THEN 'IN_PROGRESS'
                ELSE 'SUCCESS'
              END AS overall_conclusion
            FROM (
              SELECT DISTINCT ON (owner, repo, workflow_name)
                owner, repo, workflow_name, conclusion, status
              FROM pipeline_run
              WHERE branch = 'main'
              AND pr_id IS NULL
              AND workflow_name IS NOT NULL
              ORDER BY owner, repo, workflow_name, created_at DESC
            ) latest_per_workflow
            GROUP BY owner, repo
            ORDER BY owner, repo
            """, nativeQuery = true)
    List<RepoHealthSummary> findRepoHealthSummaries();

    /**
     * Returns distinct head SHAs for a pull request ordered by most recent
     * first. One entry per commit — used to paginate run history by commit set.
     */
    @Query(value = """
            SELECT head_sha
            FROM pipeline_run
            WHERE pr_id = :prId
            GROUP BY head_sha
            ORDER BY MIN(started_at) DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT head_sha)
            FROM pipeline_run
            WHERE pr_id = :prId
            """, nativeQuery = true)
    Page<String> findDistinctHeadShasByPrId(
            @Param("prId") UUID prId,
            Pageable pageable);

    /**
     * Returns all runs for a pull request whose head SHA is in the given list,
     * ordered by started_at DESC.
     */
    @Query("""
            SELECT r FROM PipelineRun r
            WHERE r.pullRequest.id = :prId
            AND r.headSha IN :headShas
            ORDER BY r.startedAt DESC
            """)
    List<PipelineRun> findByPullRequestIdAndHeadShaIn(
            @Param("prId") UUID prId,
            @Param("headShas") List<String> headShas);
}
