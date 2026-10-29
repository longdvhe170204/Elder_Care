package com.eldercare.backend.medication.internal.domain;

/**
 * Prescription suspension reason (FR-011, FR-037).
 */
public enum SuspensionReason {
    /** Suspended per physician order */
    ORDER,
    /** Suspended automatically upon admission or hospital return pending reconciliation */
    RECONCILIATION_PENDING
}
