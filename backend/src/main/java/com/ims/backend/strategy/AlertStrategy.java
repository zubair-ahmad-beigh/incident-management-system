package com.ims.backend.strategy;

import com.ims.backend.model.Severity;

/**
 * Strategy interface for determining incident severity from a signal type.
 * Add new implementations to support additional component/failure types
 * without modifying existing code (Open/Closed Principle).
 */
public interface AlertStrategy {

    /**
     * Returns true if this strategy handles the given signal type.
     */
    boolean supports(String signalType);

    /**
     * Returns the severity that should be assigned to the incident.
     */
    Severity determineSeverity(String signalType, String componentId);
}
