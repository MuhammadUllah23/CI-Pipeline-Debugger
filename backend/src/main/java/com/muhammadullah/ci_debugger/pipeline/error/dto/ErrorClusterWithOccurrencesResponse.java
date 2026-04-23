package com.muhammadullah.ci_debugger.pipeline.error.dto;

import java.util.List;

public class ErrorClusterWithOccurrencesResponse {

    private ErrorClusterResponse cluster;
    private List<ErrorOccurrenceResponse> occurrences;

    private ErrorClusterWithOccurrencesResponse() {};

    public static ErrorClusterWithOccurrencesResponse of(ErrorClusterResponse errorCluster, List<ErrorOccurrenceResponse> occurrences) {
        ErrorClusterWithOccurrencesResponse reponse = new ErrorClusterWithOccurrencesResponse();
        reponse.cluster = errorCluster;
        reponse.occurrences = occurrences;
        return reponse;
    }

    public ErrorClusterResponse getCluster() { return cluster; }
    public List<ErrorOccurrenceResponse> getOccurrences() { return occurrences; }
    
}
