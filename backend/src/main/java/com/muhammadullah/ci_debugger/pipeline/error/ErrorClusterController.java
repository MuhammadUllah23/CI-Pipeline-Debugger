package com.muhammadullah.ci_debugger.pipeline.error;

import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterResponse;
import com.muhammadullah.ci_debugger.pipeline.error.dto.ErrorClusterWithOccurrencesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clusters")
public class ErrorClusterController {

    private final ErrorClusterService errorClusterService;

    public ErrorClusterController(ErrorClusterService errorClusterService) {
        this.errorClusterService = errorClusterService;
    }

    /**
     * Returns all clusters sorted by occurrence count descending.
     * Limit defaults to 50 if not specified, clamped to 100 in the service.
     */
    @GetMapping
    public List<ErrorClusterResponse> listAll(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return errorClusterService.listAll(limit);
    }

    /**
     * Returns a single cluster by ID with all its occurrences.
     */
    @GetMapping("/{id}")
    public ErrorClusterWithOccurrencesResponse findById(@PathVariable UUID id) {
        return errorClusterService.findById(id);
    }
}