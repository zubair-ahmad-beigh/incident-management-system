package com.ims.backend.controller;

import com.ims.backend.dto.IncidentResponse;
import com.ims.backend.dto.StatusTransitionRequest;
import com.ims.backend.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incident lifecycle controller.
 *
 * GET  /api/incidents/{id}         – get single incident (cache-first)
 * GET  /api/incidents?status=OPEN  – list by status
 * GET  /api/incidents?component=x  – list by component
 * PATCH /api/incidents/{id}/status – state transition
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/{id}")
    public Mono<IncidentResponse> getIncident(@PathVariable String id) {
        return incidentService.getIncident(id);
    }

    @GetMapping
    public Flux<IncidentResponse> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String component) {

        if (status != null) {
            return incidentService.getIncidentsByStatus(status.toUpperCase());
        }
        if (component != null) {
            return incidentService.getIncidentsByComponent(component);
        }
        // Default: return OPEN incidents
        return incidentService.getIncidentsByStatus("OPEN");
    }

    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<IncidentResponse>> transitionStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusTransitionRequest request) {

        return incidentService.transitionStatus(id, request)
                .map(ResponseEntity::ok);
    }
}
