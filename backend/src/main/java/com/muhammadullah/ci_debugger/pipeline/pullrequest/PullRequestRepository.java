package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {

    /**
     * Finds an existing PR by its provider identity.
     *
     * @param provider the CI provider (e.g. "GITHUB")
     * @param owner    the repository owner
     * @param repo     the repository name
     * @param prNumber the pull request number
     * @return the existing PR if found
     */
    Optional<PullRequest> findByProviderAndOwnerAndRepoAndPrNumber(
            String provider,
            String owner,
            String repo,
            int prNumber);

    /**
     * Returns a paginated list of pull requests for a given repo filtered by state.
     * Ordered by most recently updated first.
     *
     * @param owner    the repository owner
     * @param repo     the repository name
     * @param states   the PR states to include (e.g. [OPEN] or [CLOSED, MERGED])
     * @param pageable pagination parameters
     * @return a page of matching pull requests
     */
    Page<PullRequest> findByOwnerAndRepoAndPrStateOrderByUpdatedAtDesc(
            String owner,
            String repo,
            PullRequestState state,
            Pageable pageable);
}
