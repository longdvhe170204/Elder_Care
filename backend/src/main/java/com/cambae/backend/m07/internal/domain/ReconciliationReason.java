package com.cambae.backend.m07.internal.domain;

/**
 * Reason for creating medication reconciliation (FR-034, FR-034b, Q-340).
 */
public enum ReconciliationReason {
    /** Upon resident admission */
    ADMISSION,
    /** Upon resident return from hospital treatment */
    HOSPITAL_RETURN,
    /** Professional clinical request (requires explicit reason, does not suspend prescriptions or set deadline) */
    PROFESSIONAL_REQUEST
}
