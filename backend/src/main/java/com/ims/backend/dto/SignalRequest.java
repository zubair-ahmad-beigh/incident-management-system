package com.ims.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Inbound DTO for signal ingestion.
 * POST /api/signals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalRequest {

    @NotBlank(message = "componentId must not be blank")
    private String componentId;

    @NotBlank(message = "type must not be blank")
    private String type;

    @NotBlank(message = "message must not be blank")
    private String message;

    @NotNull(message = "timestamp must be provided")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
