package com.ims.backend.state;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;

/**
 * Factory that creates the correct {@link IncidentState} implementation
 * for a given {@link IncidentStatus} string.
 */
public final class IncidentStateFactory {

    private IncidentStateFactory() {}

    public static IncidentState of(String status) {
        return of(IncidentStatus.valueOf(status));
    }

    public static IncidentState of(IncidentStatus status) {
        return switch (status) {
            case OPEN          -> new OpenState();
            case INVESTIGATING -> new InvestigatingState();
            case RESOLVED      -> new ResolvedState();
            case CLOSED        -> new ClosedState();
        };
    }
}
