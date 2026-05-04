package com.ims.backend;

import com.ims.backend.exception.InvalidStateTransitionException;
import com.ims.backend.model.IncidentStatus;
import com.ims.backend.state.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the State Pattern incident lifecycle.
 */
class IncidentStateTest {

    // ── Valid transitions ──────────────────────────────────────────────────

    @Test
    @DisplayName("OPEN → INVESTIGATING is valid")
    void openToInvestigating() {
        IncidentState state = new OpenState();
        IncidentState next  = state.transition(IncidentStatus.INVESTIGATING, false);
        assertThat(next.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
    }

    @Test
    @DisplayName("INVESTIGATING → RESOLVED is valid")
    void investigatingToResolved() {
        IncidentState state = new InvestigatingState();
        IncidentState next  = state.transition(IncidentStatus.RESOLVED, false);
        assertThat(next.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    @DisplayName("RESOLVED → CLOSED is valid when RCA exists")
    void resolvedToClosedWithRca() {
        IncidentState state = new ResolvedState();
        IncidentState next  = state.transition(IncidentStatus.CLOSED, true);
        assertThat(next.getStatus()).isEqualTo(IncidentStatus.CLOSED);
    }

    // ── Invalid transitions ────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"RESOLVED", "CLOSED"})
    @DisplayName("OPEN cannot skip to RESOLVED or CLOSED")
    void openCannotSkip(IncidentStatus target) {
        assertThatThrownBy(() -> new OpenState().transition(target, false))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("RESOLVED → CLOSED without RCA should throw")
    void resolvedToClosedWithoutRca() {
        assertThatThrownBy(() -> new ResolvedState().transition(IncidentStatus.CLOSED, false))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("RCA");
    }

    @Test
    @DisplayName("CLOSED is terminal – no transitions allowed")
    void closedIsTerminal() {
        assertThatThrownBy(() -> new ClosedState().transition(IncidentStatus.OPEN, false))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    // ── Factory ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("StateFactory creates correct implementation for each status")
    void factory_createsCorrectImpl() {
        assertThat(IncidentStateFactory.of(IncidentStatus.OPEN)).isInstanceOf(OpenState.class);
        assertThat(IncidentStateFactory.of(IncidentStatus.INVESTIGATING)).isInstanceOf(InvestigatingState.class);
        assertThat(IncidentStateFactory.of(IncidentStatus.RESOLVED)).isInstanceOf(ResolvedState.class);
        assertThat(IncidentStateFactory.of(IncidentStatus.CLOSED)).isInstanceOf(ClosedState.class);
    }
}
