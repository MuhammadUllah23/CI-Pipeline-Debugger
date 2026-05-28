package com.muhammadullah.ci_debugger.pipeline.run;

public interface RepoHealthSummary {
    String getOwner();
    String getRepo();
    String getOverallConclusion();
}
