package com.ims.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Inbound DTO for creating / updating an RCA.
 * POST /api/rca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcaRequest {

    @NotNull(message = "incidentId is required")
    private String incidentId;

    @NotBlank(message = "root_cause must not be blank")
    private String rootCause;

    @NotBlank(message = "fix_applied must not be blank")
    private String fixApplied;

    @NotBlank(message = "prevention_steps must not be blank")
    private String preventionSteps;
}
