package com.muhammadullah.ci_debugger.pipeline.run.github;

public final class GitHubWebhookConstants {

    private GitHubWebhookConstants() {}

    public static final String EVENT_WORKFLOW_RUN = "workflow_run";
    public static final String ACTION_COMPLETED = "completed";
    public static final String PROVIDER = "GITHUB";
    public static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    public static final String SIGNATURE_PREFIX = "sha256=";
}