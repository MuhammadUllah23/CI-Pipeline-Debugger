package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubStepsApiResponse {

    private List<Job> jobs;

    public List<Job> getJobs() { return jobs; }
    public void setJobs(List<Job> jobs) { this.jobs = jobs; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Job {

        private String name;
        private String status;
        private String conclusion;
        private List<Step> steps;

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getConclusion() { return conclusion; }
        public List<Step> getSteps() { return steps; }

        public void setName(String name) { this.name = name; }
        public void setStatus(String status) { this.status = status; }
        public void setConclusion(String conclusion) { this.conclusion = conclusion; }
        public void setSteps(List<Step> steps) { this.steps = steps; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Step {

            private String name;
            private String status;
            private String conclusion;
            private int number;

            @JsonProperty("started_at")
            private Instant startedAt;

            @JsonProperty("completed_at")
            private Instant completedAt;

            public String getName() { return name; }
            public String getStatus() { return status; }
            public String getConclusion() { return conclusion; }
            public int getNumber() { return number; }
            public Instant getStartedAt() { return startedAt; }
            public Instant getCompletedAt() { return completedAt; }

            public void setName(String name) { this.name = name; }
            public void setStatus(String status) { this.status = status; }
            public void setConclusion(String conclusion) { this.conclusion = conclusion; }
            public void setNumber(int number) { this.number = number; }
            public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
            public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
        }
    }
}