package com.cambae.backend.m07.internal.domain;

/**
 * Prescription action commands requiring prescriber basis (FR-013, BR-M07-16).
 */
public enum PrescriptionBasisCommand {
    SUSPEND,
    RESUME,
    DISCONTINUE,
    EXTEND,
    CHANGE_DOSE
}
