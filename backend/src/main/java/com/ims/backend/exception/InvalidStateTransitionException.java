package com.ims.backend.exception;

import com.ims.backend.model.IncidentStatus;

/**
 * Thrown when an invalid state transition is attempted.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to) {
        super(String.format("Invalid transition: %s → %s is not permitted.", from, to));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
