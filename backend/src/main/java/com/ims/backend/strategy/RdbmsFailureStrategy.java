package com.ims.backend.strategy;

import com.ims.backend.model.Severity;
import org.springframework.stereotype.Component;

/**
 * RDBMS / database failure → P0 (critical).
 * Triggered by signal types containing "DB_FAILURE" or "DATABASE_ERROR".
 */
@Component
public class RdbmsFailureStrategy implements AlertStrategy {

    private static final java.util.Set<String> HANDLED_TYPES = java.util.Set.of(
            "DB_FAILURE", "DATABASE_ERROR", "CONNECTION_POOL_EXHAUSTED"
    );

    @Override
    public boolean supports(String signalType) {
        return signalType != null && HANDLED_TYPES.contains(signalType.toUpperCase());
    }

    @Override
    public Severity determineSeverity(String signalType, String componentId) {
        return Severity.P0;  // Database failures always P0
    }
}
