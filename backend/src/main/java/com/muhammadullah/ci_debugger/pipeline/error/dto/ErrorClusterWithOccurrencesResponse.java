package com.muhammadullah.ci_debugger.pipeline.error.dto;

import com.muhammadullah.ci_debugger.pipeline.error.ErrorCluster;
import org.springframework.data.domain.Page;

public class ErrorClusterWithOccurrencesResponse {

    private ErrorClusterResponse cluster;
    private Page<ErrorOccurrenceResponse> occurrences;

    private ErrorClusterWithOccurrencesResponse() {
    }

    public static ErrorClusterWithOccurrencesResponse of(
            ErrorCluster errorCluster,
            Page<ErrorOccurrenceResponse> occurrences) {
        ErrorClusterWithOccurrencesResponse response = new ErrorClusterWithOccurrencesResponse();
        response.cluster = ErrorClusterResponse.from(errorCluster);
        response.occurrences = occurrences;
        return response;
    }

    public ErrorClusterResponse getCluster() {
        return cluster;
    }

    public Page<ErrorOccurrenceResponse> getOccurrences() {
        return occurrences;
    }
}
