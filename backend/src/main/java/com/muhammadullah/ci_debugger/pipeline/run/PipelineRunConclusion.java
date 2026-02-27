package com.muhammadullah.ci_debugger.pipeline.run;

public enum PipelineRunConclusion {
    SUCCESS,
    FAILURE,
    CANCELLED,
    SKIPPED,
    TIMED_OUT,
    NEUTRAL,
    ACTION_REQUIRED,
    STALE,      
    UNKNOWN
}
