package com.ims.backend;

import com.ims.backend.dto.RcaRequest;
import com.ims.backend.exception.RcaValidationException;
import com.ims.backend.service.RcaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RCA validation logic.
 * No Spring context – plain Mockito for fast execution.
 */
class RcaValidationTest {

    @Mock private com.ims.backend.repository.relational.RcaRepository rcaRepository;
    @Mock private com.ims.backend.repository.relational.IncidentRepository incidentRepository;

    @InjectMocks
    private RcaService rcaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── Valid RCA ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid RCA with all fields populated should pass validation")
    void validRca_shouldPassValidation() {
        RcaRequest request = RcaRequest.builder()
                .incidentId(UUID.randomUUID().toString())
                .rootCause("Database connection pool exhausted due to N+1 queries")
                .fixApplied("Optimised queries and increased pool size to 50")
                .preventionSteps("Add query analysis to CI pipeline; set pool alerts")
                .build();

        assertThatCode(() -> rcaService.validate(request)).doesNotThrowAnyException();
    }

    // ── Missing fields ─────────────────────────────────────────────────────

    @ParameterizedTest(name = "Missing {0} should fail validation")
    @MethodSource("missingFieldArgs")
    @DisplayName("Blank mandatory RCA fields should throw RcaValidationException")
    void missingField_shouldThrow(String fieldName, RcaRequest request) {
        assertThatThrownBy(() -> rcaService.validate(request))
                .isInstanceOf(RcaValidationException.class)
                .hasMessageContaining(fieldName);
    }

    static Stream<Arguments> missingFieldArgs() {
        String id = UUID.randomUUID().toString();
        return Stream.of(
                Arguments.of("root_cause", RcaRequest.builder()
                        .incidentId(id).rootCause("").fixApplied("fix").preventionSteps("step").build()),
                Arguments.of("fix_applied", RcaRequest.builder()
                        .incidentId(id).rootCause("cause").fixApplied("  ").preventionSteps("step").build()),
                Arguments.of("prevention_steps", RcaRequest.builder()
                        .incidentId(id).rootCause("cause").fixApplied("fix").preventionSteps(null).build())
        );
    }

    @Test
    @DisplayName("All blank fields should report all three violations")
    void allBlankFields_shouldReportAllViolations() {
        RcaRequest request = RcaRequest.builder()
                .incidentId(UUID.randomUUID().toString())
                .rootCause("")
                .fixApplied("")
                .preventionSteps("")
                .build();

        assertThatThrownBy(() -> rcaService.validate(request))
                .isInstanceOf(RcaValidationException.class)
                .hasMessageContaining("root_cause")
                .hasMessageContaining("fix_applied")
                .hasMessageContaining("prevention_steps");
    }

    // ── Whitespace-only ────────────────────────────────────────────────────

    @Test
    @DisplayName("Whitespace-only root_cause should be treated as blank")
    void whitespaceOnlyRootCause_shouldFail() {
        RcaRequest request = RcaRequest.builder()
                .incidentId(UUID.randomUUID().toString())
                .rootCause("   ")
                .fixApplied("Applied fix")
                .preventionSteps("Prevention steps noted")
                .build();

        assertThatThrownBy(() -> rcaService.validate(request))
                .isInstanceOf(RcaValidationException.class)
                .hasMessageContaining("root_cause");
    }
}
