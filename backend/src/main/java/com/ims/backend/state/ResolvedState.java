package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

public class ResolvedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() { return IncidentStatus.RESOLVED; }

    @Override
    public IncidentState transition(IncidentStatus target, boolean hasValidRca) {
        if (target != IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(getStatus(), target);
        }
        if (!hasValidRca) {
            throw new InvalidStateTransitionException(
                    "Cannot close incident: RCA is missing or incomplete. " +
                    "Submit a complete RCA via POST /api/rca before closing."
            );
        }
        return new ClosedState();
    }
}
