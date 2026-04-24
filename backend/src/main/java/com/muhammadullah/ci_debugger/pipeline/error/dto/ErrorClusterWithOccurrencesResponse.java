package com.muhammadullah.ci_debugger.pipeline.error.dto;

import java.util.List;

import com.muhammadullah.ci_debugger.pipeline.error.ErrorCluster;

public class ErrorClusterWithOccurrencesResponse {

    private ErrorClusterResponse cluster;
    private List<ErrorOccurrenceResponse> occurrences;

    private ErrorClusterWithOccurrencesResponse() {};

    public static ErrorClusterWithOccurrencesResponse of(ErrorCluster errorCluster, List<ErrorOccurrenceResponse> occurrences) {
        ErrorClusterWithOccurrencesResponse reponse = new ErrorClusterWithOccurrencesResponse();
        reponse.cluster = ErrorClusterResponse.from(errorCluster);
        reponse.occurrences = occurrences;
        return reponse;
    }

    public ErrorClusterResponse getCluster() { return cluster; }
    public List<ErrorOccurrenceResponse> getOccurrences() { return occurrences; }
    
}
