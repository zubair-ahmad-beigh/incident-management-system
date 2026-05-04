package com.ims.backend.strategy;

import com.ims.backend.model.Severity;
import org.springframework.stereotype.Component;

/**
 * Network / connectivity failure → P1.
 */
@Component
public class NetworkFailureStrategy implements AlertStrategy {

    private static final java.util.Set<String> HANDLED_TYPES = java.util.Set.of(
            "NETWORK_ERROR", "TIMEOUT", "CONNECTION_REFUSED", "SERVICE_UNAVAILABLE"
    );

    @Override
    public boolean supports(String signalType) {
        return signalType != null && HANDLED_TYPES.contains(signalType.toUpperCase());
    }

    @Override
    public Severity determineSeverity(String signalType, String componentId) {
        return Severity.P1;
    }
}
