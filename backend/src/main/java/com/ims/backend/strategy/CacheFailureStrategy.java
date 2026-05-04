package com.ims.backend.strategy;

import com.ims.backend.model.Severity;
import org.springframework.stereotype.Component;

/**
 * Cache failure → P2 (medium).
 * Triggered by signal types containing "CACHE_MISS" or "REDIS_ERROR".
 */
@Component
public class CacheFailureStrategy implements AlertStrategy {

    private static final java.util.Set<String> HANDLED_TYPES = java.util.Set.of(
            "CACHE_MISS", "REDIS_ERROR", "CACHE_TIMEOUT", "CACHE_FAILURE"
    );

    @Override
    public boolean supports(String signalType) {
        return signalType != null && HANDLED_TYPES.contains(signalType.toUpperCase());
    }

    @Override
    public Severity determineSeverity(String signalType, String componentId) {
        return Severity.P2;  // Cache failures are medium severity
    }
}
