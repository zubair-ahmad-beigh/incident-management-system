package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

public class InvestigatingState implements IncidentState {

    @Override
    public IncidentStatus getStatus() { return IncidentStatus.INVESTIGATING; }

    @Override
    public IncidentState transition(IncidentStatus target, boolean hasValidRca) {
        if (target != IncidentStatus.RESOLVED) {
            throw new InvalidStateTransitionException(getStatus(), target);
        }
        return new ResolvedState();
    }
}
