package com.muhammadullah.ci_debugger.pipeline.run;

import java.util.Set;

public enum PipelineRunConclusion {
    SUCCESS,
    FAILURE,
    CANCELLED,
    SKIPPED,
    TIMED_OUT,
    NEUTRAL,
    ACTION_REQUIRED,
    STALE,
    UNKNOWN;

    public static final Set<PipelineRunConclusion> FAILURE_CONCLUSIONS = Set.of(
            FAILURE,
            TIMED_OUT,
            ACTION_REQUIRED
    );
}