package com.ims.backend;

import com.ims.backend.dto.RcaRequest;
import com.ims.backend.exception.RcaValidationException;
import com.ims.backend.model.RcaRecord;
import com.ims.backend.repository.relational.IncidentRepository;
import com.ims.backend.repository.relational.RcaRepository;
import com.ims.backend.model.Incident;
import com.ims.backend.service.RcaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Reactive (StepVerifier) integration tests for RcaService.submitRca().
 * Mocks the repositories to avoid requiring a running DB.
 */
@ExtendWith(MockitoExtension.class)
class RcaServiceReactiveTest {

    @Mock private RcaRepository      rcaRepository;
    @Mock private IncidentRepository incidentRepository;
    @InjectMocks private RcaService  rcaService;

    private String incidentId;
    private Incident fakeIncident;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID().toString();
        fakeIncident = Incident.builder()
                .id(incidentId)
                .componentId("order-service")
                .status("OPEN")
                .severity("P0")
                .startTime(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Valid RCA submission persists and returns saved record")
    void submitRca_valid_returnsSaved() {
        RcaRequest req = RcaRequest.builder()
                .incidentId(incidentId)
                .rootCause("DB connection pool exhausted")
                .fixApplied("Increased pool size and fixed N+1 queries")
                .preventionSteps("Add query analysis gate to CI; alert on pool saturation > 80%")
                .build();

        RcaRecord saved = RcaRecord.builder()
                .id(UUID.randomUUID().toString())
                .incidentId(incidentId)
                .rootCause(req.getRootCause())
                .fixApplied(req.getFixApplied())
                .preventionSteps(req.getPreventionSteps())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Mono.just(fakeIncident));
        when(rcaRepository.findByIncidentId(incidentId)).thenReturn(Mono.empty());
        when(rcaRepository.save(any(RcaRecord.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(rcaService.submitRca(req))
                .expectNextMatches(r ->
                        r.getIncidentId().equals(incidentId) &&
                        r.getRootCause().equals(req.getRootCause()))
                .verifyComplete();
    }

    @Test
    @DisplayName("submitRca with blank rootCause should throw RcaValidationException synchronously")
    void submitRca_blankRootCause_throws() {
        RcaRequest req = RcaRequest.builder()
                .incidentId(incidentId)
                .rootCause("")
                .fixApplied("fix")
                .preventionSteps("steps")
                .build();

        // validate() is called before any reactive pipeline, so it throws immediately
        StepVerifier.create(Mono.fromCallable(() -> {
                    rcaService.validate(req);
                    return req;
                }))
                .expectError(RcaValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("submitRca for non-existent incident returns ResourceNotFoundException")
    void submitRca_incidentNotFound_emitsError() {
        RcaRequest req = RcaRequest.builder()
                .incidentId(incidentId)
                .rootCause("cause")
                .fixApplied("fix")
                .preventionSteps("steps")
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Mono.empty());

        StepVerifier.create(rcaService.submitRca(req))
                .expectError(com.ims.backend.exception.ResourceNotFoundException.class)
                .verify();
    }
}
