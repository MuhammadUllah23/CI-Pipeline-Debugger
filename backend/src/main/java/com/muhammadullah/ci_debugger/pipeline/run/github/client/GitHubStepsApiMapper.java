package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunConclusion;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunStatus;
import com.muhammadullah.ci_debugger.pipeline.run.PipelineRunValueMapper;
import com.muhammadullah.ci_debugger.pipeline.step.dto.PipelineStepSaveRequest;

import java.util.List;

public final class GitHubStepsApiMapper {

    private GitHubStepsApiMapper() {}

    /**
     * Maps a GitHub Steps API response to a flat list of {@link PipelineStepSaveRequest}s.
     *
     * Each job in the response is flattened into its individual steps, with the
     * job name carried onto each step so the {@code (pipeline_run_id, job_name, step_index)}
     * unique constraint can be satisfied.
     *
     * @param response the GitHub Steps API response to map
     * @return a flat list of step save requests across all jobs in the response
     */
    public static List<PipelineStepSaveRequest> toSaveRequests(GitHubStepsApiResponse response) {
        return response.getJobs().stream()
                .filter(job -> job.getSteps() != null)
                .flatMap(job -> job.getSteps().stream()
                        .map(step -> toSaveRequest(job.getName(), step)))
                .toList();
    }

    private static PipelineStepSaveRequest toSaveRequest(String jobName, GitHubStepsApiResponse.Job.Step step) {
        PipelineRunStatus status = PipelineRunValueMapper.toStatus(step.getStatus());
        PipelineRunConclusion conclusion = PipelineRunValueMapper.toConclusion(step.getConclusion());

        PipelineStepSaveRequest request = new PipelineStepSaveRequest();
        request.setJobName(jobName);
        request.setStepName(step.getName());
        request.setStepIndex(step.getNumber());
        request.setStatus(status);
        request.setConclusion(conclusion);
        request.setStartedAt(step.getStartedAt());
        request.setCompletedAt(step.getCompletedAt());
        return request;
    }
}