package com.cambae.backend.m07.internal.domain;

/**
 * Prescriber facility type (FR-005, FR-006, BR-M07-05).
 */
public enum PrescriberType {
    /** Internal facility prescription (licensed physician only) */
    INTERNAL,
    /** External medical facility prescription (Hospital, Clinic) */
    EXTERNAL
}
