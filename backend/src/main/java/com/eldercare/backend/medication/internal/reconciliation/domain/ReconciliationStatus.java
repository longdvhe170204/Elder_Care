package com.eldercare.backend.medication.internal.reconciliation.domain;

/**
 * Medication reconciliation process workflow status (FR-035).
 */
public enum ReconciliationStatus {
    /** Open, allows editing and adding reconciliation lines */
    OPEN,
    /** Doctor saved partial decisions as draft */
    DRAFT,
    /** Submitted, pending review and confirmation by a second staff member */
    PENDING_CONFIRMATION,
    /** Confirmed, decisions applied to prescriptions */
    CONFIRMED,
    /** Cancelled (re-hospitalization, admission cancellation, or final discharge state) */
    CANCELLED
}
