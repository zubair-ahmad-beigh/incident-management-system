package com.ims.backend.model;

/**
 * Incident lifecycle states.
 * Valid transitions:
 *   OPEN → INVESTIGATING → RESOLVED → CLOSED
 * CLOSED requires a completed RCA.
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    CLOSED;

    /**
     * Checks whether a transition from this state to {@code next} is valid.
     */
    public boolean canTransitionTo(IncidentStatus next) {
        return switch (this) {
            case OPEN          -> next == INVESTIGATING;
            case INVESTIGATING -> next == RESOLVED;
            case RESOLVED      -> next == CLOSED;
            case CLOSED        -> false;
        };
    }
}
