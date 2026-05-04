package com.ims.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published after a signal is accepted.
 * Consumed by SignalConsumer for async processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalEvent {
    private String signalId;
    private String componentId;
    private String type;
    private String message;
    private String rawPayload;
    private Instant timestamp;
}
