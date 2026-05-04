package com.ims.backend.controller;

import com.ims.backend.dto.RcaRequest;
import com.ims.backend.model.RcaRecord;
import com.ims.backend.service.RcaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * RCA controller.
 *
 * POST /api/rca         – submit (create or update) an RCA
 * GET  /api/rca/{incidentId} – retrieve RCA for an incident
 */
@RestController
@RequestMapping("/api/rca")
@RequiredArgsConstructor
public class RcaController {

    private final RcaService rcaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RcaRecord> submitRca(@Valid @RequestBody RcaRequest request) {
        return rcaService.submitRca(request);
    }

    @GetMapping("/{incidentId}")
    public Mono<RcaRecord> getRca(@PathVariable String incidentId) {
        return rcaService.getRca(incidentId);
    }
}
