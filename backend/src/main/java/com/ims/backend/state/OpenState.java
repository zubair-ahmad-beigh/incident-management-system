package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

public class OpenState implements IncidentState {

    @Override
    public IncidentStatus getStatus() { return IncidentStatus.OPEN; }

    @Override
    public IncidentState transition(IncidentStatus target, boolean hasValidRca) {
        if (target != IncidentStatus.INVESTIGATING) {
            throw new InvalidStateTransitionException(getStatus(), target);
        }
        return new InvestigatingState();
    }
}
