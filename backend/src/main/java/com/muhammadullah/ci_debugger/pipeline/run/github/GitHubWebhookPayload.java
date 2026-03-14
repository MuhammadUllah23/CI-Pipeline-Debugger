package com.muhammadullah.ci_debugger.pipeline.run.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubWebhookPayload {

    private String action;

    @JsonProperty("workflow_run")
    private WorkflowRun workflowRun;

    private Repository repository;

    public String getAction() { return action; }
    public WorkflowRun getWorkflowRun() { return workflowRun; }
    public Repository getRepository() { return repository; }

    public void setAction(String action) { this.action = action; }
    public void setWorkflowRun(WorkflowRun workflowRun) { this.workflowRun = workflowRun; }
    public void setRepository(Repository repository) { this.repository = repository; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowRun {

        private Long id;
        private String name;
        private String status;
        private String conclusion;

        @JsonProperty("head_sha")
        private String headSha;

        @JsonProperty("head_branch")
        private String headBranch;

        @JsonProperty("run_started_at")
        private Instant runStartedAt;

        //** used as completedAt when action == "completed" — GitHub has no dedicated completion timestamp
        @JsonProperty("updated_at")
        private Instant updatedAt;

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getConclusion() { return conclusion; }
        public String getHeadSha() { return headSha; }
        public String getHeadBranch() { return headBranch; }
        public Instant getRunStartedAt() { return runStartedAt; }
        public Instant getUpdatedAt() { return updatedAt; }

        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setStatus(String status) { this.status = status; }
        public void setConclusion(String conclusion) { this.conclusion = conclusion; }
        public void setHeadSha(String headSha) { this.headSha = headSha; }
        public void setHeadBranch(String headBranch) { this.headBranch = headBranch; }
        public void setRunStartedAt(Instant runStartedAt) { this.runStartedAt = runStartedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {

        private String name;
        private Owner owner;

        public String getName() { return name; }
        public Owner getOwner() { return owner; }

        public void setName(String name) { this.name = name; }
        public void setOwner(Owner owner) { this.owner = owner; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Owner {

            private String login;

            public String getLogin() { return login; }
            public void setLogin(String login) { this.login = login; }
        }
    }
}