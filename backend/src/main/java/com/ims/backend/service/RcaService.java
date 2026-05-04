package com.ims.backend.service;

import com.ims.backend.dto.RcaRequest;
import com.ims.backend.exception.RcaValidationException;
import com.ims.backend.exception.ResourceNotFoundException;
import com.ims.backend.model.RcaRecord;
import com.ims.backend.repository.relational.IncidentRepository;
import com.ims.backend.repository.relational.RcaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RCA service.
 *
 * Validates that all three mandatory RCA fields are non-empty before persisting.
 * Supports upsert: if an RCA already exists for the incident it is updated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RcaService {

    private final RcaRepository      rcaRepository;
    private final IncidentRepository incidentRepository;

    /**
     * Validates and upserts an RCA record.
     * Throws {@link RcaValidationException} if validation fails.
     * Throws {@link ResourceNotFoundException} if the incident does not exist.
     */
    @Transactional
    @CircuitBreaker(name = "mysqlDB")
    @Retry(name = "mysqlDB")
    public Mono<RcaRecord> submitRca(RcaRequest request) {
        validate(request);

        return incidentRepository.findById(request.getIncidentId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Incident", request.getIncidentId())))
                .flatMap(incident ->
                        rcaRepository.findByIncidentId(request.getIncidentId())
                                .defaultIfEmpty(RcaRecord.builder()
                                        .id(UUID.randomUUID().toString())
                                        .incidentId(request.getIncidentId())
                                        .createdAt(Instant.now())
                                        .build())
                                .map(existing -> {
                                    existing.setRootCause(request.getRootCause());
                                    existing.setFixApplied(request.getFixApplied());
                                    existing.setPreventionSteps(request.getPreventionSteps());
                                    existing.setUpdatedAt(Instant.now());
                                    return existing;
                                })
                                .flatMap(rcaRepository::save)
                )
                .doOnSuccess(rca -> log.info("RCA saved for incident {}", rca.getIncidentId()));
    }

    /** Retrieves the RCA for an incident. */
    public Mono<RcaRecord> getRca(String incidentId) {
        return rcaRepository.findByIncidentId(incidentId)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("RCA for incident", incidentId)));
    }

    // ─────────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────────

    /**
     * Validates all mandatory RCA fields.
     * Collects all violations before throwing so the caller gets a full report.
     */
    public void validate(RcaRequest request) {
        List<String> violations = new ArrayList<>();

        if (!StringUtils.hasText(request.getRootCause())) {
            violations.add("root_cause must not be blank");
        }
        if (!StringUtils.hasText(request.getFixApplied())) {
            violations.add("fix_applied must not be blank");
        }
        if (!StringUtils.hasText(request.getPreventionSteps())) {
            violations.add("prevention_steps must not be blank");
        }

        if (!violations.isEmpty()) {
            throw new RcaValidationException("RCA validation failed: " + String.join("; ", violations));
        }
    }
}
