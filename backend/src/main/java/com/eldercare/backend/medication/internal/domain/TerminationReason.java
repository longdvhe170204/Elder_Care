package com.eldercare.backend.medication.internal.domain;

/**
 * Prescription termination reason (FR-011).
 */
public enum TerminationReason {
    /** Discontinued by physician order */
    DISCONTINUED,
    /** Replaced by new prescription (dosage or medication change) */
    REPLACED,
    /** Expired automatically past end date */
    EXPIRED,
    /** Terminated via medication reconciliation outcome */
    RECONCILIATION,
    /** Resident deceased or residence terminated */
    FINAL_STATE
}
