package com.muhammadullah.ci_debugger.pipeline.run;

import java.util.Locale;

/**
 * Maps raw string values from provider webhook payloads to their corresponding
 * internal enum representations.
 *
 * <p>All mappings are case-insensitive and trim whitespace before comparing.
 * Unknown or unrecognised values are mapped to their respective {@code UNKNOWN}
 * sentinel rather than throwing an exception — callers should treat {@code UNKNOWN}
 * as a signal that the value was present but not recognised, not that it was absent.
 *
 */
public final class PipelineRunValueMapper {

    private PipelineRunValueMapper() {}

    /**
     * Maps a raw status string from a provider payload to a {@link PipelineRunStatus}.
     *
     * @param raw the raw status string from the provider payload, may be {@code null}
     * @return the corresponding {@link PipelineRunStatus}, never {@code null}
     */
    public static PipelineRunStatus toStatus(String raw) {
        if (raw == null) return PipelineRunStatus.UNKNOWN;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "queued" -> PipelineRunStatus.QUEUED;
            case "in_progress" -> PipelineRunStatus.IN_PROGRESS;
            case "completed" -> PipelineRunStatus.COMPLETED;
            default -> PipelineRunStatus.UNKNOWN;
        };
    }

    /**
     * Maps a raw conclusion string from a provider payload to a {@link PipelineRunConclusion}.
     *
     * @param raw the raw conclusion string from the provider payload, may be {@code null}
     * @return the corresponding {@link PipelineRunConclusion}, or {@code null} if
     *         {@code raw} is {@code null}
     */
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
