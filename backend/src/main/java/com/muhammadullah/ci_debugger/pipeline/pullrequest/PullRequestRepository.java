package com.muhammadullah.ci_debugger.pipeline.pullrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {

    /**
     * Finds an existing PR by its provider identity.
     *
     * @param provider  the CI provider (e.g. "GITHUB")
     * @param owner     the repository owner
     * @param repo      the repository name
     * @param prNumber  the pull request number
     * @return the existing PR if found
     */
    Optional<PullRequest> findByProviderAndOwnerAndRepoAndPrNumber(
            String provider,
            String owner,
            String repo,
            int prNumber
    );
}
