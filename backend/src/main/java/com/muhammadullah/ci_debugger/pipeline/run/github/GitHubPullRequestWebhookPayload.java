package com.muhammadullah.ci_debugger.pipeline.run.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestWebhookPayload {

    private String action;

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    private Repository repository;

    public String getAction() {
        return action;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {

        private int number;
        private String state;

        @JsonProperty("merged_at")
        private Instant mergedAt;

        public int getNumber() {
            return number;
        }

        public String getState() {
            return state;
        }

        public Instant getMergedAt() {
            return mergedAt;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public void setState(String state) {
            this.state = state;
        }

        public void setMergedAt(Instant mergedAt) {
            this.mergedAt = mergedAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {

        private String name;
        private Owner owner;

        public String getName() {
            return name;
        }

        public Owner getOwner() {
            return owner;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setOwner(Owner owner) {
            this.owner = owner;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Owner {

            private String login;

            public String getLogin() {
                return login;
            }

            public void setLogin(String login) {
                this.login = login;
            }
        }
    }
}
