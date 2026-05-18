package com.muhammadullah.ci_debugger.pipeline.pullrequest.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestApiResponse {

    private int number;
    private String title;
    private String state;

    @JsonProperty("merged_at")
    private Instant mergedAt;

    @JsonProperty("head")
    private Head head;

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getState() {
        return state;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public Head getHead() {
        return head;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setMergedAt(Instant mergedAt) {
        this.mergedAt = mergedAt;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Head {

        private String sha;
        private String ref;

        public String getSha() {
            return sha;
        }

        public String getRef() {
            return ref;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }
    }
}
