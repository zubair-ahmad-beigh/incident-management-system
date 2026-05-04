package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

/**
 * State pattern interface for incident lifecycle management.
 * Each concrete state encapsulates valid transitions and guards.
 */
public interface IncidentState {

    IncidentStatus getStatus();

    /**
     * Transitions to {@code target}. Throws if the transition is invalid.
     *
     * @param target       the desired next status
     * @param hasValidRca  whether a completed RCA record exists (required for CLOSED)
     * @return             the resulting new state
     */
    IncidentState transition(IncidentStatus target, boolean hasValidRca);
}
