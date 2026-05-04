package com.ims.backend.model;

/**
 * Severity levels for incidents.
 * P0 = critical, P1 = high, P2 = medium.
 * Assigned by the AlertStrategy based on signal type.
 */
public enum Severity {
    P0, P1, P2
}
