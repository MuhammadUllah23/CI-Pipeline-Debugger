package com.muhammadullah.ci_debugger.pipeline.run;

import java.util.Locale;

public final class PipelineRunValueMapper {

    private PipelineRunValueMapper() {}

    public static PipelineRunStatus toStatus(String raw) {
        if (raw == null) return PipelineRunStatus.UNKNOWN;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "queued" -> PipelineRunStatus.QUEUED;
            case "in_progress" -> PipelineRunStatus.IN_PROGRESS;
            case "completed" -> PipelineRunStatus.COMPLETED;
            default -> PipelineRunStatus.UNKNOWN;
        };
    }

    public static PipelineRunConclusion toConclusion(String raw) {
        if (raw == null) return null;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "success" -> PipelineRunConclusion.SUCCESS;
            case "failure" -> PipelineRunConclusion.FAILURE;
            case "cancelled" -> PipelineRunConclusion.CANCELLED;
            case "skipped" -> PipelineRunConclusion.SKIPPED;
            case "timed_out" -> PipelineRunConclusion.TIMED_OUT;
            case "neutral" -> PipelineRunConclusion.NEUTRAL;
            case "action_required" -> PipelineRunConclusion.ACTION_REQUIRED;
            case "stale" -> PipelineRunConclusion.STALE;
            default -> PipelineRunConclusion.UNKNOWN;
        };
    }
}
