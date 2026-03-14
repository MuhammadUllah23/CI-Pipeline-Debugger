package com.muhammadullah.ci_debugger.pipeline.run.github;

import com.muhammadullah.ci_debugger.pipeline.run.dto.PipelineRunUpsertRequest;

public final class GitHubWebhookMapper {

    private GitHubWebhookMapper() {}

    public static PipelineRunUpsertRequest toUpsertRequest(GitHubWebhookPayload payload) {
        GitHubWebhookPayload.WorkflowRun run = payload.getWorkflowRun();
        GitHubWebhookPayload.Repository repo = payload.getRepository();

        PipelineRunUpsertRequest req = new PipelineRunUpsertRequest();

        req.setProvider(GitHubWebhookConstants.PROVIDER);
        req.setOwner(repo.getOwner().getLogin());
        req.setRepo(repo.getName());
        req.setProviderRunId(String.valueOf(run.getId()));

        req.setWorkflowName(run.getName());
        req.setStatus(run.getStatus());
        req.setHeadSha(run.getHeadSha());
        req.setBranch(run.getHeadBranch());
        req.setStartedAt(run.getRunStartedAt());

        if (GitHubWebhookConstants.ACTION_COMPLETED.equals(payload.getAction())) {
            req.setConclusion(run.getConclusion());
            req.setCompletedAt(run.getUpdatedAt());
        }

        return req;
    }
}