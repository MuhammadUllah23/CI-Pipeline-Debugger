package com.muhammadullah.ci_debugger.pipeline.run;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {

    Optional<PipelineRun> findByProviderAndOwnerAndRepoAndProviderRunId(
            PipelineRunProvider provider,
            String owner,
            String repo,
            String providerRunId
    );

}
