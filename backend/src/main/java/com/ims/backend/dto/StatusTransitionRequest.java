package com.ims.backend.dto;

import com.ims.backend.model.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for transitioning an incident to a new status.
 * PATCH /api/incidents/{id}/status
 */
@Data
public class StatusTransitionRequest {

    @NotNull(message = "targetStatus is required")
    private IncidentStatus targetStatus;
}
