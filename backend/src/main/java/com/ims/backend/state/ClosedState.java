package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

public class ClosedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() { return IncidentStatus.CLOSED; }

    @Override
    public IncidentState transition(IncidentStatus target, boolean hasValidRca) {
        throw new InvalidStateTransitionException(
                "Incident is already CLOSED. No further transitions are permitted."
        );
    }
}
